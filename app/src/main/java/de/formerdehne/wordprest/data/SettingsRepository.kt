package de.formerdehne.wordprest.data

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
    val displayName: String?,
    val maxImageEdge: Int,
    val maxUploadMb: Int,
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("wordprest", Context.MODE_PRIVATE)
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
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            maxImageEdge = prefs.getInt(KEY_MAX_EDGE, DEFAULT_MAX_EDGE),
            maxUploadMb = prefs.getInt(KEY_MAX_UPLOAD, DEFAULT_MAX_UPLOAD_MB),
        )
    }

    fun saveCredentials(credentials: Credentials, siteName: String, displayName: String) {
        prefs.edit()
            .putString(KEY_URL, credentials.siteUrl)
            .putString(KEY_USER, credentials.username)
            .putString(KEY_PASSWORD, SecretBox.encrypt(credentials.appPassword))
            .putBoolean(KEY_REST_ROUTE, credentials.useRestRoute)
            .putString(KEY_SITE_NAME, siteName)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
        _state.value = load()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_URL).remove(KEY_USER).remove(KEY_PASSWORD)
            .remove(KEY_REST_ROUTE).remove(KEY_SITE_NAME).remove(KEY_DISPLAY_NAME)
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
        private const val KEY_MAX_EDGE = "max_image_edge"
        private const val KEY_MAX_UPLOAD = "max_upload_mb"
    }
}
