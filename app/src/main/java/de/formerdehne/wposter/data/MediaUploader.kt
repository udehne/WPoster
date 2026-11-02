package de.formerdehne.wposter.data

import android.content.Context
import android.net.Uri
import de.formerdehne.wposter.R
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

/** Aufbereiten + Hochladen einer Mediendatei. Wird vom Erstellen- und vom Bearbeiten-Editor genutzt. */
class MediaUploader(
    private val context: Context,
    private val settings: SettingsRepository,
    private val network: NetworkMonitor,
) {

    private val preparer = MediaPreparer(context)

    suspend fun stage(uri: Uri): Uri = preparer.stage(uri)

    fun releaseStaged(uri: Uri) = MediaPreparer.deleteStaged(context, uri)

    /** Nacheinander hochladen schont Akku und Webserver. */
    val lock = Mutex()

    /**
     * Lädt die Datei in den Blog [accountId] hoch. Ohne Internet wird gewartet, bis wieder
     * eine Verbindung besteht ([onWaitingForNetwork]); Verbindungsabbrüche werden wiederholt.
     */
    suspend fun upload(
        uri: Uri,
        accountId: String?,
        onWaitingForNetwork: () -> Unit,
        onPreparing: () -> Unit,
        onUploading: () -> Unit,
        onProgress: (Float) -> Unit,
    ): UploadedMedia {
        var failures = 0
        while (true) {
            if (!network.online.value) {
                onWaitingForNetwork()
                network.online.first { it }
            }
            try {
                return uploadOnce(uri, accountId, onPreparing, onUploading, onProgress)
            } catch (e: WpException) {
                if (!e.isNetwork) throw e
                failures++
                // Trotz Netz mehrfach gescheitert (z. B. Server nicht erreichbar) → Fehler anzeigen.
                if (network.online.value && failures >= MAX_NETWORK_RETRIES) throw e
                onWaitingForNetwork()
                delay(failures * 3000L)
            }
        }
    }

    private suspend fun uploadOnce(
        uri: Uri,
        accountId: String?,
        onPreparing: () -> Unit,
        onUploading: () -> Unit,
        onProgress: (Float) -> Unit,
    ): UploadedMedia {
        val current = settings.state.value
        val account = current.account(accountId) ?: current.active
            ?: throw WpException(str(R.string.error_credentials_missing))
        onPreparing()
        val prepared = preparer.prepare(uri, current.maxImageEdge)
        try {
            val sizeMb = prepared.file.length() / (1024.0 * 1024.0)
            if (sizeMb > account.maxUploadMb) {
                throw WpException(
                    str(R.string.error_file_too_large, "%.0f".format(sizeMb), account.maxUploadMb)
                )
            }
            onUploading()
            return WpClient(account.credentials).uploadMedia(prepared.file, prepared.mimeType, prepared.fileName, onProgress)
        } finally {
            prepared.file.delete()
        }
    }

    companion object {
        private const val MAX_NETWORK_RETRIES = 4
    }
}
