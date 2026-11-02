package de.formerdehne.wposter.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Credentials(
    val siteUrl: String,
    val username: String,
    val appPassword: String,
    /** true, wenn die Seite keine "schönen" Permalinks hat und ?rest_route= nötig ist. */
    val useRestRoute: Boolean,
)

/** Ein eingerichteter Blog mit Zugangsdaten. */
data class SiteAccount(
    val id: String,
    val credentials: Credentials,
    val siteName: String,
    val siteDescription: String,
    val displayName: String,
    val maxUploadMb: Int = SettingsRepository.DEFAULT_MAX_UPLOAD_MB,
) {
    val host: String get() = credentials.siteUrl.removePrefix("https://").removePrefix("http://")
}

data class AppSettings(
    val accounts: List<SiteAccount>,
    val activeId: String?,
    /** Blog, der beim Start der App angezeigt wird. */
    val startId: String?,
    val maxImageEdge: Int,
    val checkUpdates: Boolean,
    /** Zuletzt bei GitHub gefundene Version (für den Update-Hinweis). */
    val latestVersion: String?,
    val latestUrl: String?,
    val dismissedVersion: String?,
    val lastUpdateCheck: Long,
) {
    val active: SiteAccount? get() = accounts.find { it.id == activeId } ?: accounts.firstOrNull()
    val credentials: Credentials? get() = active?.credentials
    val siteName: String? get() = active?.siteName
    val siteDescription: String? get() = active?.siteDescription
    val displayName: String? get() = active?.displayName
    val maxUploadMb: Int get() = active?.maxUploadMb ?: SettingsRepository.DEFAULT_MAX_UPLOAD_MB

    fun account(id: String?): SiteAccount? = accounts.find { it.id == id }
}

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("wposter", Context.MODE_PRIVATE)
    private val _state: MutableStateFlow<AppSettings>
    val state: StateFlow<AppSettings>

    init {
        migrateSingleAccount()
        // Beim Start der App: festgelegten Start-Blog aktivieren.
        prefs.getString(KEY_START_ID, null)?.let { prefs.edit().putString(KEY_ACTIVE_ID, it).apply() }
        _state = MutableStateFlow(load())
        state = _state.asStateFlow()
    }

    private fun load(): AppSettings {
        val accounts = readAccounts()
        val activeId = prefs.getString(KEY_ACTIVE_ID, null)?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id
        val startId = prefs.getString(KEY_START_ID, null)?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id
        return AppSettings(
            accounts = accounts,
            activeId = activeId,
            startId = startId,
            maxImageEdge = prefs.getInt(KEY_MAX_EDGE, DEFAULT_MAX_EDGE),
            checkUpdates = prefs.getBoolean(KEY_CHECK_UPDATES, true),
            latestVersion = prefs.getString(KEY_LATEST_VERSION, null),
            latestUrl = prefs.getString(KEY_LATEST_URL, null),
            dismissedVersion = prefs.getString(KEY_DISMISSED_VERSION, null),
            lastUpdateCheck = prefs.getLong(KEY_LAST_CHECK, 0L),
        )
    }

    // ---------- Blogs ----------

    private fun readAccounts(): List<SiteAccount> {
        val arr = try { JSONArray(prefs.getString(KEY_ACCOUNTS, "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val j = arr.optJSONObject(i) ?: return@mapNotNull null
            // Passwort nicht entschlüsselbar (z. B. nach Gerätewechsel) → Blog muss neu eingerichtet werden.
            val password = SecretBox.decrypt(j.optString("pw")) ?: return@mapNotNull null
            SiteAccount(
                id = j.getString("id"),
                credentials = Credentials(j.getString("url"), j.getString("user"), password, j.optBoolean("restRoute")),
                siteName = j.optString("name"),
                siteDescription = j.optString("desc"),
                displayName = j.optString("display"),
                maxUploadMb = j.optInt("maxUpload", DEFAULT_MAX_UPLOAD_MB),
            )
        }
    }

    private fun writeAccounts(accounts: List<SiteAccount>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("url", a.credentials.siteUrl)
                    .put("user", a.credentials.username)
                    .put("pw", SecretBox.encrypt(a.credentials.appPassword))
                    .put("restRoute", a.credentials.useRestRoute)
                    .put("name", a.siteName)
                    .put("desc", a.siteDescription)
                    .put("display", a.displayName)
                    .put("maxUpload", a.maxUploadMb)
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }

    /** Blog hinzufügen oder (gleiche id) aktualisieren. Liefert die id. Neue Blogs werden aktiv. */
    fun saveAccount(
        id: String?,
        credentials: Credentials,
        siteName: String,
        siteDescription: String,
        displayName: String,
    ): String {
        val accounts = readAccounts().toMutableList()
        val existing = accounts.indexOfFirst { it.id == id }
        val newId = id ?: UUID.randomUUID().toString()
        val account = SiteAccount(
            id = newId,
            credentials = credentials,
            siteName = siteName,
            siteDescription = siteDescription,
            displayName = displayName,
            maxUploadMb = if (existing >= 0) accounts[existing].maxUploadMb else DEFAULT_MAX_UPLOAD_MB,
        )
        if (existing >= 0) accounts[existing] = account else accounts += account
        writeAccounts(accounts)
        val edit = prefs.edit()
        if (existing < 0) edit.putString(KEY_ACTIVE_ID, newId)
        if (accounts.size == 1) edit.putString(KEY_START_ID, newId)
        edit.apply()
        _state.value = load()
        return newId
    }

    fun removeAccount(id: String) {
        writeAccounts(readAccounts().filterNot { it.id == id })
        val edit = prefs.edit()
        if (prefs.getString(KEY_ACTIVE_ID, null) == id) edit.remove(KEY_ACTIVE_ID)
        if (prefs.getString(KEY_START_ID, null) == id) edit.remove(KEY_START_ID)
        edit.apply()
        _state.value = load()
    }

    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
        _state.value = load()
    }

    fun setStart(id: String) {
        prefs.edit().putString(KEY_START_ID, id).apply()
        _state.value = load()
    }

    /** Titel/Untertitel eines Blogs aktualisieren (z. B. nach Änderung in WordPress). */
    fun updateSiteMeta(accountId: String, name: String, description: String) {
        if (name.isBlank()) return
        val accounts = readAccounts()
        val a = accounts.find { it.id == accountId } ?: return
        if (a.siteName == name && a.siteDescription == description) return
        writeAccounts(accounts.map { if (it.id == accountId) it.copy(siteName = name, siteDescription = description) else it })
        _state.value = load()
    }

    fun setMaxUploadMb(accountId: String, value: Int) {
        writeAccounts(readAccounts().map { if (it.id == accountId) it.copy(maxUploadMb = value.coerceIn(1, 4096)) else it })
        _state.value = load()
    }

    /** Übernahme der Zugangsdaten aus Version 1.x (nur ein Blog). */
    private fun migrateSingleAccount() {
        val url = prefs.getString(KEY_URL, null) ?: return
        if (prefs.contains(KEY_ACCOUNTS)) return
        val user = prefs.getString(KEY_USER, null)
        val password = prefs.getString(KEY_PASSWORD, null)?.let { SecretBox.decrypt(it) }
        if (user != null && password != null) {
            val id = UUID.randomUUID().toString()
            writeAccounts(
                listOf(
                    SiteAccount(
                        id = id,
                        credentials = Credentials(url, user, password, prefs.getBoolean(KEY_REST_ROUTE, false)),
                        siteName = prefs.getString(KEY_SITE_NAME, null).orEmpty(),
                        siteDescription = prefs.getString(KEY_SITE_DESCRIPTION, null).orEmpty(),
                        displayName = prefs.getString(KEY_DISPLAY_NAME, null).orEmpty(),
                        maxUploadMb = prefs.getInt(KEY_MAX_UPLOAD, DEFAULT_MAX_UPLOAD_MB),
                    )
                )
            )
            prefs.edit().putString(KEY_ACTIVE_ID, id).putString(KEY_START_ID, id).apply()
        }
        prefs.edit()
            .remove(KEY_URL).remove(KEY_USER).remove(KEY_PASSWORD).remove(KEY_REST_ROUTE)
            .remove(KEY_SITE_NAME).remove(KEY_SITE_DESCRIPTION).remove(KEY_DISPLAY_NAME).remove(KEY_MAX_UPLOAD)
            .apply()
    }

    // ---------- Allgemeine Einstellungen ----------

    fun setMaxImageEdge(value: Int) {
        prefs.edit().putInt(KEY_MAX_EDGE, value).apply()
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

        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_ACTIVE_ID = "active_id"
        private const val KEY_START_ID = "start_id"
        // Alte Schlüssel (Version 1.x, nur für die Übernahme)
        private const val KEY_URL = "site_url"
        private const val KEY_USER = "username"
        private const val KEY_PASSWORD = "app_password"
        private const val KEY_REST_ROUTE = "use_rest_route"
        private const val KEY_SITE_NAME = "site_name"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_SITE_DESCRIPTION = "site_description"
        private const val KEY_MAX_UPLOAD = "max_upload_mb"
        private const val KEY_CHECK_UPDATES = "check_updates"
        private const val KEY_LATEST_VERSION = "latest_version"
        private const val KEY_LATEST_URL = "latest_url"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val KEY_MAX_EDGE = "max_image_edge"
    }
}
