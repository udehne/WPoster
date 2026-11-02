package de.formerdehne.wposter.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.BlockBuilder
import de.formerdehne.wposter.data.MediaUploader
import de.formerdehne.wposter.data.PublishedPost
import de.formerdehne.wposter.data.UploadedMedia
import de.formerdehne.wposter.data.WpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class ItemState { WAITING, PREPARING, UPLOADING, DONE, FAILED }

data class MediaItem(
    val key: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val state: ItemState = ItemState.WAITING,
    val progress: Float = 0f,
    val error: String? = null,
    val uploaded: UploadedMedia? = null,
)

data class DraftState(
    val title: String = "",
    val before: String = "",
    val after: String = "",
    val items: List<MediaItem> = emptyList(),
    val publishing: Boolean = false,
    val publishError: String? = null,
    val published: PublishedPost? = null,
) {
    val hasContent get() = title.isNotBlank() || before.isNotBlank() || after.isNotBlank() || items.isNotEmpty()
    val pendingUploads get() = items.count { it.state != ItemState.DONE && it.state != ItemState.FAILED }
    val failedUploads get() = items.count { it.state == ItemState.FAILED }
    val canPublish get() = title.isNotBlank() && !publishing && pendingUploads == 0 && failedUploads == 0
}

class ComposeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WPosterApp).settings
    private val uploader = MediaUploader(app, settings)
    private val jobs = HashMap<Long, Job>()
    private var nextKey = 1L

    private val _state = MutableStateFlow(DraftState())
    val state: StateFlow<DraftState> = _state.asStateFlow()

    fun setTitle(v: String) = _state.update { it.copy(title = v, publishError = null) }
    fun setBefore(v: String) = _state.update { it.copy(before = v) }
    fun setAfter(v: String) = _state.update { it.copy(after = v) }

    /** [stage] = true für per "Teilen" erhaltene Dateien: erst in den App-Speicher kopieren. */
    fun addMedia(uris: List<Uri>, stage: Boolean = false) {
        val resolver = getApplication<Application>().contentResolver
        val known = _state.value.items.map { it.uri }.toSet()
        val newItems = uris.filter { it !in known }.map { uri ->
            MediaItem(
                key = nextKey++,
                uri = uri,
                isVideo = resolver.getType(uri)?.startsWith("video/") == true,
            )
        }
        if (newItems.isEmpty()) return
        _state.update { it.copy(items = it.items + newItems, published = null) }
        if (!stage) {
            newItems.forEach { startUpload(it.key) }
            return
        }
        newItems.forEach { item ->
            jobs[item.key] = viewModelScope.launch {
                try {
                    val staged = uploader.stage(item.uri)
                    updateItem(item.key) { it.copy(uri = staged) }
                    jobs.remove(item.key)
                    startUpload(item.key)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    updateItem(item.key) {
                        it.copy(state = ItemState.FAILED, error = "Die Datei konnte nicht gelesen werden.")
                    }
                }
            }
        }
    }

    private fun startUpload(key: Long) {
        jobs[key]?.cancel()
        updateItem(key) { it.copy(state = ItemState.WAITING, error = null, progress = 0f) }
        jobs[key] = viewModelScope.launch {
            // Nacheinander hochladen: schont Handy-Akku und Webserver.
            uploader.lock.withLock { upload(key) }
        }
    }

    private suspend fun upload(key: Long) {
        val item = _state.value.items.find { it.key == key } ?: return
        try {
            var lastPercent = -1
            val uploaded = uploader.upload(
                item.uri,
                onPreparing = { updateItem(key) { it.copy(state = ItemState.PREPARING) } },
                onUploading = { updateItem(key) { it.copy(state = ItemState.UPLOADING, progress = 0f) } },
                onProgress = { p ->
                    val percent = (p * 100).toInt()
                    if (percent != lastPercent) {
                        lastPercent = percent
                        updateItem(key) { it.copy(progress = p) }
                    }
                },
            )
            updateItem(key) { it.copy(state = ItemState.DONE, progress = 1f, uploaded = uploaded) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateItem(key) { it.copy(state = ItemState.FAILED, error = e.message ?: e.javaClass.simpleName) }
        }
    }

    fun dismissError() = _state.update { it.copy(publishError = null) }

    fun retry(key: Long) = startUpload(key)

    fun retryFailed() {
        _state.value.items.filter { it.state == ItemState.FAILED }.forEach { startUpload(it.key) }
    }

    fun remove(key: Long) {
        jobs.remove(key)?.cancel()
        val item = _state.value.items.find { it.key == key } ?: return
        _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }
        uploader.releaseStaged(item.uri)
        item.uploaded?.let { deleteRemote(listOf(it.id)) }
    }

    fun move(key: Long, delta: Int) = _state.update { s ->
        val list = s.items.toMutableList()
        val from = list.indexOfFirst { it.key == key }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@update s
        list.add(to, list.removeAt(from))
        s.copy(items = list)
    }

    fun publish() {
        val s = _state.value
        if (!s.canPublish) return
        val credentials = settings.state.value.credentials ?: run {
            _state.update { it.copy(publishError = "Bitte zuerst die Zugangsdaten eintragen.") }
            return
        }
        _state.update { it.copy(publishing = true, publishError = null) }
        viewModelScope.launch {
            try {
                val media = s.items.mapNotNull { it.uploaded }
                val content = BlockBuilder.build(s.before, media, s.after)
                val featured = media.firstOrNull { it.isImage }?.id
                val post = WpClient(credentials).createPost(s.title.trim(), content, featured)
                // Entwurf leeren, Ergebnis für die Erfolgsseite behalten.
                _state.value.items.forEach { uploader.releaseStaged(it.uri) }
                _state.value = DraftState(published = post)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(publishing = false, publishError = e.message ?: "Unbekannter Fehler") }
            }
        }
    }

    /** Neuer, leerer Entwurf (z. B. nach erfolgreichem Veröffentlichen). */
    fun reset() {
        _state.value.items.forEach { uploader.releaseStaged(it.uri) }
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _state.value = DraftState()
    }

    /** Entwurf verwerfen und bereits hochgeladene Dateien wieder löschen. */
    fun discard() {
        val uploadedIds = _state.value.items.mapNotNull { it.uploaded?.id }
        reset()
        deleteRemote(uploadedIds)
    }

    private fun deleteRemote(ids: List<Long>) {
        val credentials = settings.state.value.credentials ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                ids.forEach { id -> runCatching { WpClient(credentials).deleteMedia(id) } }
            }
        }
    }

    private fun updateItem(key: Long, transform: (MediaItem) -> MediaItem) = _state.update { s ->
        s.copy(items = s.items.map { if (it.key == key) transform(it) else it })
    }
}
