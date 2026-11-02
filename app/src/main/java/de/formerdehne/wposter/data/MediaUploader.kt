package de.formerdehne.wposter.data

import android.content.Context
import android.net.Uri
import de.formerdehne.wposter.R
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.sync.Mutex

/** Aufbereiten + Hochladen einer Mediendatei. Wird vom Erstellen- und vom Bearbeiten-Editor genutzt. */
class MediaUploader(private val context: Context, private val settings: SettingsRepository) {

    private val preparer = MediaPreparer(context)

    suspend fun stage(uri: Uri): Uri = preparer.stage(uri)

    fun releaseStaged(uri: Uri) = MediaPreparer.deleteStaged(context, uri)

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
            ?: throw WpException(str(R.string.error_credentials_missing))
        onPreparing()
        val prepared = preparer.prepare(uri, current.maxImageEdge)
        try {
            val sizeMb = prepared.file.length() / (1024.0 * 1024.0)
            if (sizeMb > current.maxUploadMb) {
                throw WpException(
                    str(R.string.error_file_too_large, "%.0f".format(sizeMb), current.maxUploadMb)
                )
            }
            onUploading()
            return WpClient(credentials).uploadMedia(prepared.file, prepared.mimeType, prepared.fileName, onProgress)
        } finally {
            prepared.file.delete()
        }
    }
}
