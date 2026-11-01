package de.formerdehne.wposter.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Mutex

/** Aufbereiten + Hochladen einer Mediendatei. Wird vom Erstellen- und vom Bearbeiten-Editor genutzt. */
class MediaUploader(context: Context, private val settings: SettingsRepository) {

    private val preparer = MediaPreparer(context)

    /** Nacheinander hochladen schont Akku und Webserver. */
    val lock = Mutex()

    suspend fun upload(
        uri: Uri,
        onPreparing: () -> Unit,
        onUploading: () -> Unit,
        onProgress: (Float) -> Unit,
    ): UploadedMedia {
        val current = settings.state.value
        val credentials = current.credentials
            ?: throw WpException("Bitte zuerst die Zugangsdaten in den Einstellungen eintragen.")
        onPreparing()
        val prepared = preparer.prepare(uri, current.maxImageEdge)
        try {
            val sizeMb = prepared.file.length() / (1024.0 * 1024.0)
            if (sizeMb > current.maxUploadMb) {
                throw WpException(
                    "Datei ist zu groß (%.0f MB, erlaubt sind %d MB).".format(sizeMb, current.maxUploadMb)
                )
            }
            onUploading()
            return WpClient(credentials).uploadMedia(prepared.file, prepared.mimeType, prepared.fileName, onProgress)
        } finally {
            prepared.file.delete()
        }
    }
}
