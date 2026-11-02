package de.formerdehne.wposter.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Credentials(
    val siteUrl: String,
    val username: String,
    val appPassword: String,
    /** true, wenn die Seite keine "schönen" Permalinks hat und ?rest_route= nötig ist. */
    val useRestRoute: Boolean,
)

data class AppSettings(
    val credentials: Credentials?,
    val siteName: String?,
    val siteDescription: String?,
    val displayName: String?,
    val maxImageEdge: Int,
    val maxUploadMb: Int,
    val checkUpdates: Boolean,
    /** Zuletzt bei GitHub gefundene Version (für den Update-Hinweis). */
    val latestVersion: String?,
    val latestUrl: String?,
    val dismissedVersion: String?,
    val lastUpdateCheck: Long,
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("wposter", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings {
        val url = prefs.getString(KEY_URL, null)
        val user = prefs.getString(KEY_USER, null)
        val password = prefs.getString(KEY_PASSWORD, null)?.let { SecretBox.decrypt(it) }
        val credentials = if (url != null && user != null && password != null) {
            Credentials(url, user, password, prefs.getBoolean(KEY_REST_ROUTE, false))
        } else null
        return AppSettings(
            credentials = credentials,
            siteName = prefs.getString(KEY_SITE_NAME, null),
            siteDescription = prefs.getString(KEY_SITE_DESCRIPTION, null),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            maxImageEdge = prefs.getInt(KEY_MAX_EDGE, DEFAULT_MAX_EDGE),
            maxUploadMb = prefs.getInt(KEY_MAX_UPLOAD, DEFAULT_MAX_UPLOAD_MB),
            checkUpdates = prefs.getBoolean(KEY_CHECK_UPDATES, true),
            latestVersion = prefs.getString(KEY_LATEST_VERSION, null),
            latestUrl = prefs.getString(KEY_LATEST_URL, null),
            dismissedVersion = prefs.getString(KEY_DISMISSED_VERSION, null),
            lastUpdateCheck = prefs.getLong(KEY_LAST_CHECK, 0L),
        )
    }

    fun saveCredentials(credentials: Credentials, siteName: String, siteDescription: String, displayName: String) {
        prefs.edit()
            .putString(KEY_URL, credentials.siteUrl)
            .putString(KEY_USER, credentials.username)
            .putString(KEY_PASSWORD, SecretBox.encrypt(credentials.appPassword))
            .putBoolean(KEY_REST_ROUTE, credentials.useRestRoute)
            .putString(KEY_SITE_NAME, siteName)
            .putString(KEY_SITE_DESCRIPTION, siteDescription)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
        _state.value = load()
    }

    /** Titel/Untertitel des Blogs aktualisieren (z. B. nach Änderung in WordPress). */
    fun updateSiteMeta(name: String, description: String) {
        if (name.isBlank()) return
        val current = _state.value
        if (current.siteName == name && current.siteDescription == description) return
        prefs.edit().putString(KEY_SITE_NAME, name).putString(KEY_SITE_DESCRIPTION, description).apply()
        _state.value = load()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_URL).remove(KEY_USER).remove(KEY_PASSWORD)
            .remove(KEY_REST_ROUTE).remove(KEY_SITE_NAME).remove(KEY_SITE_DESCRIPTION).remove(KEY_DISPLAY_NAME)
            .apply()
        _state.value = load()
    }

    fun setMaxImageEdge(value: Int) {
        prefs.edit().putInt(KEY_MAX_EDGE, value).apply()
        _state.value = load()
    }

    fun setMaxUploadMb(value: Int) {
        prefs.edit().putInt(KEY_MAX_UPLOAD, value.coerceIn(1, 4096)).apply()
        _state.value = load()
    }

    fun setCheckUpdates(value: Boolean) {
        prefs.edit().putBoolean(KEY_CHECK_UPDATES, value).apply()
        _state.value = load()
    }

    fun saveUpdateCheck(version: String?, url: String?) {
        prefs.edit()
            .putString(KEY_LATEST_VERSION, version)
            .putString(KEY_LATEST_URL, url)
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
        _state.value = load()
    }

    fun dismissUpdate(version: String) {
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
        _state.value = load()
    }

    companion object {
        const val DEFAULT_MAX_EDGE = 2560
        const val DEFAULT_MAX_UPLOAD_MB = 128
        val IMAGE_EDGE_OPTIONS = listOf(1920, 2560, 3840)

        private const val KEY_URL = "site_url"
        private const val KEY_USER = "username"
        private const val KEY_PASSWORD = "app_password"
        private const val KEY_REST_ROUTE = "use_rest_route"
        private const val KEY_SITE_NAME = "site_name"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_SITE_DESCRIPTION = "site_description"
        private const val KEY_CHECK_UPDATES = "check_updates"
        private const val KEY_LATEST_VERSION = "latest_version"
        private const val KEY_LATEST_URL = "latest_url"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val KEY_MAX_EDGE = "max_image_edge"
        private const val KEY_MAX_UPLOAD = "max_upload_mb"
    }
}
