package de.formerdehne.wposter.data

import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.R
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials as OkCredentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WpException(
    message: String,
    val httpCode: Int? = null,
    val wpCode: String? = null,
    val notJson: Boolean = false,
) : IOException(message)

data class SiteInfo(
    val siteName: String,
    val siteDescription: String,
    val displayName: String,
    val useRestRoute: Boolean,
    val siteUrl: String,
)

data class UploadedMedia(
    val id: Long,
    val sourceUrl: String,
    /** URL der Größe "large", falls WordPress eine erzeugt hat. */
    val largeUrl: String?,
    val isImage: Boolean,
    val isVideo: Boolean,
)

data class PublishedPost(val id: Long, val link: String, val title: String)

/** Spricht die WordPress-REST-API mit Anwendungspasswort (HTTP Basic Auth) an. */
class WpClient(private val credentials: Credentials) {

    private val authHeader = OkCredentials.basic(credentials.username, credentials.appPassword, Charsets.UTF_8)

    private fun url(route: String, query: Map<String, String> = emptyMap()): HttpUrl {
        val base = credentials.siteUrl.toHttpUrlOrNull()
            ?: throw WpException(str(R.string.error_invalid_address, credentials.siteUrl))
        val builder = base.newBuilder()
        if (credentials.useRestRoute) {
            if (base.pathSegments.lastOrNull() != "") builder.addPathSegment("")
            builder.addQueryParameter("rest_route", route)
        } else {
            builder.addPathSegments("wp-json" + route)
        }
        query.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build()
    }

    private fun request(url: HttpUrl) = Request.Builder()
        .url(url)
        .header("Authorization", authHeader)
        .header("Accept", "application/json")
        .header("User-Agent", USER_AGENT)

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).await().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFor(response.code, body)
            if (!body.trimStart().let { it.startsWith("{") || it.startsWith("[") }) {
                throw WpException(
                    str(R.string.error_not_wp_api),
                    httpCode = response.code, notJson = true
                )
            }
            body
        }
    }

    private suspend fun get(route: String, query: Map<String, String> = emptyMap()): String =
        execute(request(url(route, query)).get().build())

    /** Prüft die Zugangsdaten und liefert Seiten-Infos + Anzeigenamen. */
    suspend fun verify(): Pair<SiteMeta, String> {
        val me = JSONObject(get("/wp/v2/users/me", mapOf("context" to "edit")))
        val caps = me.optJSONObject("capabilities")
        if (caps != null) {
            if (!caps.optBoolean("upload_files")) {
                throw WpException(str(R.string.error_no_upload_permission))
            }
            if (!caps.optBoolean("publish_posts")) {
                throw WpException(str(R.string.error_no_publish_permission))
            }
        }
        val displayName = me.optString("name").ifBlank { credentials.username }
        val meta = try { siteMeta() } catch (e: Exception) { SiteMeta("", "") }
        val name = meta.name.ifBlank { credentials.siteUrl.toHttpUrlOrNull()?.host ?: credentials.siteUrl }
        return meta.copy(name = name) to displayName
    }

    suspend fun siteMeta(): SiteMeta {
        val root = JSONObject(get("/"))
        return SiteMeta(
            HtmlText.decode(root.optString("name")).trim(),
            HtmlText.decode(root.optString("description")).trim(),
        )
    }

    /**
     * Neueste Beiträge zuerst. Liefert eine leere Liste hinter der letzten Seite.
     * [search] durchsucht Überschrift und Text; [after]/[before] grenzen den Zeitraum ein (exklusiv).
     */
    suspend fun listPosts(
        page: Int,
        perPage: Int = 20,
        search: String? = null,
        after: String? = null,
        before: String? = null,
    ): List<PostSummary> {
        val query = buildMap {
            put("per_page", perPage.toString())
            put("page", page.toString())
            put("orderby", "date")
            put("order", "desc")
            put("_embed", "author,wp:featuredmedia")
            if (!search.isNullOrBlank()) put("search", search.trim())
            if (after != null) put("after", after)
            if (before != null) put("before", before)
        }
        val body = try {
            postsQuery(query)
        } catch (e: WpException) {
            if (e.wpCode == "rest_post_invalid_page_number") return emptyList()
            throw e
        }
        val arr = JSONArray(body)
        return (0 until arr.length()).map { PostJson.summary(arr.getJSONObject(it)) }
    }

    /** Abfrage inkl. geplanter Beiträge; fällt auf "nur veröffentlicht" zurück, wenn der Server das ablehnt. */
    private suspend fun postsQuery(query: Map<String, String>): String {
        if (includeFuture) {
            try {
                return get("/wp/v2/posts", query + ("status" to "publish,future"))
            } catch (e: WpException) {
                if (e.wpCode != "rest_invalid_param" && e.wpCode != "rest_forbidden_status") throw e
                includeFuture = false
            }
        }
        return get("/wp/v2/posts", query)
    }

    suspend fun getPost(id: Long): PostDetail =
        PostJson.detail(JSONObject(get("/wp/v2/posts/$id", mapOf("_embed" to "author"))))

    suspend fun getPostForEdit(id: Long): PostEditData = try {
        PostJson.edit(JSONObject(get("/wp/v2/posts/$id", mapOf("context" to "edit"))))
    } catch (e: WpException) {
        if (e.wpCode == "rest_forbidden_context" || e.httpCode == 403) {
            throw WpException(str(R.string.error_no_edit_permission))
        }
        throw e
    }

    /** IDs des nächstälteren und nächstneueren Beitrags. */
    suspend fun neighbors(dateRaw: String): Pair<Long?, Long?> {
        val base = mapOf("per_page" to "1", "_fields" to "id", "orderby" to "date")
        fun first(body: String): Long? =
            JSONArray(body).optJSONObject(0)?.optLong("id")?.takeIf { it > 0 }
        val older = first(postsQuery(base + mapOf("before" to dateRaw, "order" to "desc")))
        val newer = first(postsQuery(base + mapOf("after" to dateRaw, "order" to "asc")))
        return older to newer
    }

    suspend fun updatePost(id: Long, title: String, content: String, date: String?, featuredMediaId: Long) {
        val payload = JSONObject()
            .put("title", title)
            .put("content", content)
            .put("featured_media", featuredMediaId)
        if (date != null) payload.put("date", date)
        val req = request(url("/wp/v2/posts/$id"))
            .post(payload.toString().toRequestBody(JSON))
            .build()
        execute(req)
    }

    suspend fun uploadMedia(
        file: File,
        mimeType: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): UploadedMedia {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val req = request(url("/wp/v2/media"))
            .header("Content-Disposition", "attachment; filename=\"$safeName\"")
            .post(ProgressRequestBody(file, mimeType.toMediaType(), onProgress))
            .build()
        val json = JSONObject(execute(req))
        val mime = json.optString("mime_type")
        val large = json.optJSONObject("media_details")
            ?.optJSONObject("sizes")
            ?.optJSONObject("large")
            ?.optString("source_url")
            ?.takeIf { it.isNotBlank() }
        return UploadedMedia(
            id = json.getLong("id"),
            sourceUrl = json.getString("source_url"),
            largeUrl = large,
            isImage = mime.startsWith("image/"),
            isVideo = mime.startsWith("video/"),
        )
    }

    suspend fun deleteMedia(id: Long) {
        // POST + _method=DELETE funktioniert auch bei Hostern, die DELETE blockieren.
        val req = request(url("/wp/v2/media/$id", mapOf("force" to "true", "_method" to "DELETE")))
            .post(ByteArray(0).toRequestBody(null))
            .build()
        execute(req)
    }

    suspend fun createPost(title: String, content: String, featuredMediaId: Long?): PublishedPost {
        val payload = JSONObject()
            .put("title", title)
            .put("content", content)
            .put("status", "publish")
        if (featuredMediaId != null) payload.put("featured_media", featuredMediaId)
        val req = request(url("/wp/v2/posts"))
            .post(payload.toString().toRequestBody(JSON))
            .build()
        val json = JSONObject(execute(req))
        return PublishedPost(
            id = json.getLong("id"),
            link = json.optString("link"),
            title = json.optJSONObject("title")?.optString("rendered") ?: title,
        )
    }

    companion object {
        private val USER_AGENT = "WPoster/${BuildConfig.VERSION_NAME} (Android)"

        /** Wird false, wenn der Server status=publish,future ablehnt. */
        @Volatile
        private var includeFuture = true
        private val JSON = "application/json; charset=utf-8".toMediaType()

        val http: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()

        /** Macht aus Eingaben wie "udehne.de/" eine saubere Basis-URL "https://udehne.de". */
        fun normalizeSiteUrl(input: String): String? {
            var s = input.trim()
            if (s.isEmpty()) return null
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "https://$s"
            val parsed = s.toHttpUrlOrNull() ?: return null
            val path = parsed.encodedPath.trimEnd('/')
            return parsed.newBuilder().encodedPath(if (path.isEmpty()) "/" else path).query(null).fragment(null)
                .build().toString().trimEnd('/')
        }

        /**
         * Prüft die Verbindung. Probiert zuerst /wp-json/ und – falls die Seite
         * keine Permalinks nutzt – danach ?rest_route=.
         */
        suspend fun probe(siteUrl: String, username: String, appPassword: String): SiteInfo {
            val base = normalizeSiteUrl(siteUrl) ?: throw WpException(str(R.string.error_enter_valid_address))
            var lastError: WpException? = null
            for (restRoute in listOf(false, true)) {
                val client = WpClient(Credentials(base, username.trim(), appPassword.trim(), restRoute))
                try {
                    val (meta, displayName) = client.verify()
                    return SiteInfo(meta.name, meta.description, displayName, restRoute, base)
                } catch (e: WpException) {
                    if (e.notJson || e.httpCode == 404) {
                        lastError = e
                        continue
                    }
                    throw e
                }
            }
            throw WpException(str(R.string.error_no_rest_api, lastError?.message ?: ""))
        }

        fun errorFor(code: Int, body: String): WpException {
            val json = try { JSONObject(body) } catch (e: Exception) { null }
            val wpCode = json?.optString("code")?.takeIf { it.isNotBlank() }
            val wpMessage = json?.optString("message")
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val text = when {
                wpCode in setOf("incorrect_password", "invalid_username", "invalid_email", "authentication_failed") ->
                    str(R.string.error_wrong_credentials)
                wpCode == "application_passwords_disabled" ->
                    str(R.string.error_app_passwords_disabled)
                wpCode == "rest_not_logged_in" || (code == 401 && wpCode == null) ->
                    str(R.string.error_auth_not_received)
                wpCode == "rest_cannot_create" || wpCode == "rest_cannot_publish" ->
                    str(R.string.error_no_permission)
                wpCode == "rest_upload_user_quota_exceeded" ->
                    str(R.string.error_quota_exceeded)
                code == 413 -> str(R.string.error_file_too_large_server)
                json == null -> str(R.string.error_unexpected_response, code)
                else -> wpMessage ?: str(R.string.error_server, code)
            }
            return WpException(text, httpCode = code, wpCode = wpCode, notJson = json == null)
        }
    }
}

private class ProgressRequestBody(
    private val file: File,
    private val type: MediaType,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = type
    override fun contentLength() = file.length()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1)
        var written = 0L
        file.source().use { source ->
            val buffer = Buffer()
            while (true) {
                val read = source.read(buffer, 64 * 1024L)
                if (read == -1L) break
                sink.write(buffer, read)
                written += read
                onProgress(written.toFloat() / total)
            }
        }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) {
                cont.resumeWithException(
                    WpException(str(R.string.error_no_connection, e.message ?: e.javaClass.simpleName))
                )
            }
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}
