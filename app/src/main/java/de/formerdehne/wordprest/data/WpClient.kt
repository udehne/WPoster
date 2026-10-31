package de.formerdehne.wordprest.data

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

data class SiteInfo(val siteName: String, val displayName: String, val useRestRoute: Boolean, val siteUrl: String)

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
            ?: throw WpException("Ungültige Adresse: ${credentials.siteUrl}")
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
                    "Der Server hat keine gültige Antwort der WordPress-API geliefert.",
                    httpCode = response.code, notJson = true
                )
            }
            body
        }
    }

    /** Prüft die Zugangsdaten und liefert Seitennamen + Anzeigenamen. */
    suspend fun verify(): Pair<String, String> {
        val me = JSONObject(execute(request(url("/wp/v2/users/me", mapOf("context" to "edit"))).get().build()))
        val caps = me.optJSONObject("capabilities")
        if (caps != null) {
            if (!caps.optBoolean("upload_files")) {
                throw WpException("Dein WordPress-Benutzer darf keine Medien hochladen (mindestens Rolle „Autor“ nötig).")
            }
            if (!caps.optBoolean("publish_posts")) {
                throw WpException("Dein WordPress-Benutzer darf keine Beiträge veröffentlichen (mindestens Rolle „Autor“ nötig).")
            }
        }
        val displayName = me.optString("name").ifBlank { credentials.username }
        val siteName = try {
            JSONObject(execute(request(url("/")).get().build())).optString("name")
        } catch (e: Exception) {
            ""
        }.ifBlank { credentials.siteUrl.toHttpUrlOrNull()?.host ?: credentials.siteUrl }
        return siteName to displayName
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
        private const val USER_AGENT = "WordPREST/1.0 (Android)"
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
            val base = normalizeSiteUrl(siteUrl) ?: throw WpException("Bitte eine gültige Adresse eingeben, z. B. https://beispiel.de")
            var lastError: WpException? = null
            for (restRoute in listOf(false, true)) {
                val client = WpClient(Credentials(base, username.trim(), appPassword.trim(), restRoute))
                try {
                    val (siteName, displayName) = client.verify()
                    return SiteInfo(siteName, displayName, restRoute, base)
                } catch (e: WpException) {
                    if (e.notJson || e.httpCode == 404) {
                        lastError = e
                        continue
                    }
                    throw e
                }
            }
            throw WpException(
                "Unter dieser Adresse wurde keine WordPress-REST-API gefunden. " +
                    "Stimmt die Adresse? (${lastError?.message ?: ""})"
            )
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
                    "Benutzername oder Anwendungspasswort ist falsch."
                wpCode == "application_passwords_disabled" ->
                    "Anwendungspasswörter sind auf dieser WordPress-Seite deaktiviert."
                wpCode == "rest_not_logged_in" || (code == 401 && wpCode == null) ->
                    "Die Anmeldung kommt beim Server nicht an. Häufige Ursache: Der Webserver " +
                        "entfernt den „Authorization“-Header. Abhilfe siehe Anleitung (README, Abschnitt .htaccess)."
                wpCode == "rest_cannot_create" || wpCode == "rest_cannot_publish" ->
                    "Keine Berechtigung für diese Aktion. Hat dein WordPress-Benutzer mindestens die Rolle „Autor“?"
                wpCode == "rest_upload_user_quota_exceeded" ->
                    "Der Speicherplatz für Uploads auf dem Server ist erschöpft."
                code == 413 -> "Die Datei ist zu groß für den Server."
                json == null -> "Unerwartete Antwort vom Server (HTTP $code)."
                else -> wpMessage ?: "Fehler vom Server (HTTP $code)."
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
                    WpException("Keine Verbindung zum Server: ${e.message ?: e.javaClass.simpleName}")
                )
            }
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}
