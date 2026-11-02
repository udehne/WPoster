package de.formerdehne.wposter.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.R
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.Credentials
import de.formerdehne.wposter.data.HtmlText
import de.formerdehne.wposter.data.WpClient
import de.formerdehne.wposter.data.WpException
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class OnboardingState(
    /** 1 = Adresse, 2 = Zugriff erlauben, 3 = Zugangsdaten. */
    val step: Int = 1,
    val url: String = "",
    /** In Schritt 1 erkannter Name des Blogs (falls die Seite ihn verrät). */
    val siteName: String? = null,
    /** Erkannte Adresse von WordPress' "Anwendung autorisieren"-Seite. */
    val authorizeUrl: String? = null,
    val user: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** Nach erfolgreichem Speichern gesetzt (einmalig, siehe [OnboardingViewModel.consumeDone]). */
    val doneAccountId: String? = null,
    val doneSiteName: String = "",
    /** true, wenn das der erste eingerichtete Blog war. */
    val wasFirst: Boolean = false,
)

/** Geführte Einrichtung eines Blogs in drei Schritten. */
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WPosterApp).settings
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    private var job: Job? = null

    /** Von vorn beginnen (alles zurücksetzen). */
    fun start() {
        job?.cancel()
        job = null
        _state.value = OnboardingState()
    }

    fun setUrl(v: String) = _state.update { it.copy(url = v, error = null) }
    fun setUser(v: String) = _state.update { it.copy(user = v, error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    /** Schritt 1: Adresse prüfen → Schritt 2. Schritt 2: manueller Weg → Schritt 3. Schritt 3: verbinden. */
    fun next() {
        val s = _state.value
        if (s.busy) return
        when (s.step) {
            1 -> checkAddress()
            2 -> _state.update { it.copy(step = 3, error = null) }
            else -> connect()
        }
    }

    /** Einen Schritt zurück; false, wenn schon bei Schritt 1. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step <= 1) return false
        job?.cancel()
        job = null
        _state.update { it.copy(step = it.step - 1, busy = false, error = null) }
        return true
    }

    private fun checkAddress() {
        val base = WpClient.normalizeSiteUrl(_state.value.url)
        if (base == null) {
            _state.update { it.copy(error = str(R.string.error_enter_valid_address)) }
            return
        }
        _state.update { it.copy(url = base, busy = true, error = null) }
        job = viewModelScope.launch {
            try {
                val discovery = discover(base)
                _state.update {
                    it.copy(
                        step = 2,
                        busy = false,
                        siteName = discovery.first,
                        authorizeUrl = discovery.second ?: defaultAuthorizeUrl(base),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update {
                    it.copy(
                        busy = false,
                        error = str(R.string.error_no_connection, e.message ?: e.javaClass.simpleName),
                    )
                }
            } catch (e: Exception) {
                // Unerwartete Antwort – trotzdem weiter, die Anmeldung im Browser klärt den Rest.
                _state.update { it.copy(step = 2, busy = false, authorizeUrl = defaultAuthorizeUrl(base)) }
            }
        }
    }

    /**
     * Fragt ohne Anmeldung den Einstiegspunkt der REST-API ab und liefert
     * (Name des Blogs, Autorisierungs-Adresse). Verlangt die Seite eine Anmeldung
     * oder antwortet nicht mit JSON, sind beide Werte null. Nur echte
     * Verbindungsprobleme werfen eine [IOException].
     */
    private suspend fun discover(base: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val first = fetchJson("$base/wp-json/")
        // Seiten ohne "schöne" Permalinks kennen nur ?rest_route=.
        val root = first ?: try { fetchJson("$base/?rest_route=/") } catch (_: IOException) { null }
        if (root == null) return@withContext null to null
        val name = HtmlText.decode(root.optString("name")).trim().takeIf { it.isNotEmpty() }
        val authorize = root.optJSONObject("authentication")
            ?.optJSONObject("application-passwords")
            ?.optJSONObject("endpoints")
            ?.optString("authorization")
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        name to authorize
    }

    /** GET ohne Anmeldung; null, wenn die Antwort kein JSON-Objekt ist. Wirft nur bei Verbindungsproblemen. */
    private fun fetchJson(url: String): JSONObject? {
        val request = try {
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
        } catch (_: IllegalArgumentException) {
            return null
        }
        val body = probeHttp.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string().orEmpty().trim() else ""
        }
        if (!body.startsWith("{")) return null
        return try { JSONObject(body) } catch (_: Exception) { null }
    }

    /** Adresse für WordPress' eingebauten Ablauf "Anwendung autorisieren" (im externen Browser öffnen). */
    fun authorizeInBrowserUrl(): String {
        val s = _state.value
        val base = WpClient.normalizeSiteUrl(s.url) ?: s.url.trim()
        val authorize = s.authorizeUrl ?: defaultAuthorizeUrl(base)
        return Uri.parse(authorize).buildUpon()
            .appendQueryParameter("app_name", APP_NAME)
            .appendQueryParameter("app_id", APP_ID)
            .appendQueryParameter("success_url", CALLBACK_URL)
            .appendQueryParameter("reject_url", "$CALLBACK_URL?rejected=1")
            .build()
            .toString()
    }

    /** Profilseite in WordPress, Abschnitt "Anwendungspasswörter". */
    fun profileUrl(): String {
        val s = _state.value
        val base = WpClient.normalizeSiteUrl(s.url) ?: s.url.trim()
        return "$base/wp-admin/profile.php#application-passwords-section"
    }

    /**
     * Rückruf von WordPress: wposter://auth?site_url=…&user_login=…&password=…
     * bzw. wposter://auth?rejected=1&success=false, wenn der Zugriff abgelehnt wurde.
     */
    fun handleAuthCallback(uri: Uri) {
        val rejected = uri.getQueryParameter("rejected") != null || uri.getQueryParameter("success") == "false"
        val password = uri.getQueryParameter("password")
        if (rejected || password == null || password.isBlank()) {
            _state.update {
                it.copy(step = if (it.url.isBlank()) 1 else 2, busy = false, error = str(R.string.onb_error_rejected))
            }
            return
        }
        val user = uri.getQueryParameter("user_login").orEmpty()
        val siteUrl = uri.getQueryParameter("site_url")
        _state.update {
            it.copy(
                step = 3,
                url = if (it.url.isBlank() && siteUrl != null && siteUrl.isNotBlank()) {
                    WpClient.normalizeSiteUrl(siteUrl) ?: siteUrl
                } else it.url,
                user = user.ifBlank { it.user },
                password = password,
                busy = false,
                error = null,
            )
        }
        connect()
    }

    /** Schritt 3: Zugangsdaten prüfen und Blog speichern. */
    fun connect() {
        val s = _state.value
        if (s.busy) return
        if (s.url.isBlank() || s.user.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = str(R.string.error_credentials_missing_short)) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        job = viewModelScope.launch {
            try {
                val user = s.user.trim()
                val password = s.password.trim()
                val info = WpClient.probe(s.url, user, password)
                val wasFirst = settings.state.value.accounts.isEmpty()
                val id = settings.saveAccount(
                    null,
                    Credentials(info.siteUrl, user, password, info.useRestRoute),
                    info.siteName,
                    info.siteDescription,
                    info.displayName,
                )
                _state.update {
                    it.copy(
                        url = info.siteUrl,
                        busy = false,
                        doneAccountId = id,
                        doneSiteName = info.siteName,
                        wasFirst = wasFirst,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: WpException) {
                _state.update { it.copy(busy = false, error = e.message ?: str(R.string.common_unknown_error)) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: str(R.string.common_unknown_error)) }
            }
        }
    }

    /** Erfolgsmeldung wurde verarbeitet. */
    fun consumeDone() = _state.update { it.copy(doneAccountId = null) }

    private fun defaultAuthorizeUrl(base: String) = "$base/wp-admin/authorize-application.php"

    companion object {
        private const val APP_NAME = "WPoster"

        /** Feste Kennung der App, damit WordPress Anwendungspasswörter von WPoster zuordnen kann. */
        private const val APP_ID = "7c3e9a52-4f1d-4b8e-a6d2-91f0c5b8e374"
        private const val CALLBACK_URL = "wposter://auth"
        private val USER_AGENT = "WPoster/${BuildConfig.VERSION_NAME} (Android)"

        /** Wie [WpClient.http], aber mit kurzen Zeitlimits – die Adressprüfung soll nicht minutenlang hängen. */
        private val probeHttp by lazy {
            WpClient.http.newBuilder()
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
