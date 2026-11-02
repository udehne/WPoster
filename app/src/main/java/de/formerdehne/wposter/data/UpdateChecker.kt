package de.formerdehne.wposter.data

import de.formerdehne.wposter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class AppUpdate(val version: String, val pageUrl: String, val apkUrl: String?)

/** Fragt bei GitHub nach, ob es eine neuere WPoster-Version gibt. */
object UpdateChecker {

    const val REPO = "udehne/WPoster"
    const val REPO_URL = "https://github.com/$REPO"

    /** Entwicklungs-Builds ("dev.42") prüfen nicht auf Updates. */
    val enabledForThisBuild: Boolean get() = !BuildConfig.VERSION_NAME.startsWith("dev")

    suspend fun latest(): AppUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "WPoster/${BuildConfig.VERSION_NAME} (Android)")
            .build()
        WpClient.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val json = JSONObject(response.body?.string().orEmpty())
            val version = json.optString("tag_name").removePrefix("v").ifBlank { return@withContext null }
            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .mapNotNull { assets?.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
                ?.optString("browser_download_url")
            AppUpdate(version, json.optString("html_url").ifBlank { "$REPO_URL/releases/latest" }, apk)
        }
    }

    /** "1.10" > "1.9", "1.4" > "1.3.2" usw. */
    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
