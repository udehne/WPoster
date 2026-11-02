package de.formerdehne.wposter.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.R
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.BlockBuilder
import de.formerdehne.wposter.data.MediaCaption
import de.formerdehne.wposter.data.MediaUploader
import de.formerdehne.wposter.data.PublishedPost
import de.formerdehne.wposter.data.UploadedMedia
import de.formerdehne.wposter.data.WpClient
import de.formerdehne.wposter.i18n.str
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

/** OFFLINE = wartet auf eine Internetverbindung. */
enum class ItemState { WAITING, OFFLINE, PREPARING, UPLOADING, DONE, FAILED }

data class MediaItem(
    val key: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val state: ItemState = ItemState.WAITING,
    val progress: Float = 0f,
    val error: String? = null,
    val uploaded: UploadedMedia? = null,
    val caption: MediaCaption = MediaCaption(),
)

data class DraftState(
    /** Blog, zu dem dieser Entwurf gehört (wird beim ersten Inhalt festgelegt). */
    val siteId: String? = null,
    val title: String = "",
    val before: String = "",
    val after: String = "",
    val items: List<MediaItem> = emptyList(),
    /** true = sofort veröffentlichen (Standard), false = als Entwurf speichern. */
    val publishNow: Boolean = true,
    val publishing: Boolean = false,
    val publishError: String? = null,
    val published: PublishedPost? = null,
) {
    val hasContent get() = title.isNotBlank() || before.isNotBlank() || after.isNotBlank() || items.isNotEmpty()
    val pendingUploads get() = items.count { it.state != ItemState.DONE && it.state != ItemState.FAILED }
    val failedUploads get() = items.count { it.state == ItemState.FAILED }
    val waitingForNetwork get() = items.any { it.state == ItemState.OFFLINE }
    val canPublish get() = title.isNotBlank() && !publishing && pendingUploads == 0 && failedUploads == 0
}

class ComposeViewModel(app: Application) : AndroidViewModel(app) {

    private val wposter = app as WPosterApp
    private val settings = wposter.settings
    private val uploader = MediaUploader(app, settings, wposter.network)
    private val jobs = HashMap<Long, Job>()
    private var nextKey = 1L

    private val _state = MutableStateFlow(DraftState())
    val state: StateFlow<DraftState> = _state.asStateFlow()

    /** Geteilte Dateien, die auf die Blog-Auswahl warten. */
    var pendingShare: List<Uri> = emptyList()

    /** Kategorien & Schlagwörter des Entwurfs. */
    val terms = TermsEditor(viewModelScope, wposter.terms) { ensureSite() }

    /** Entwurf an den aktiven Blog binden, sobald er Inhalt bekommt. */
    private fun ensureSite(): String? {
        _state.value.siteId?.let { return it }
        val id = settings.state.value.active?.id ?: return null
        _state.update { it.copy(siteId = id) }
        return id
    }

    /**
     * Entwurf für einen bestimmten Blog beginnen (z. B. nach Auswahl beim Teilen).
     * Gehört ein bestehender Entwurf zu einem anderen Blog, wird er verworfen.
     */
    fun startFor(siteId: String) {
        val s = _state.value
        if (s.siteId != null && s.siteId != siteId && s.hasContent) discard()
        _state.update { it.copy(siteId = siteId) }
        loadTerms()
    }

    /** Kategorien/Schlagwörter des Blogs laden (beim Öffnen des Editors). */
    fun loadTerms() {
        val id = _state.value.siteId ?: settings.state.value.active?.id ?: return
        terms.load(id)
    }

    fun setTitle(v: String) {
        ensureSite()
        _state.update { it.copy(title = v, publishError = null) }
    }

    fun setBefore(v: String) {
        ensureSite()
        _state.update { it.copy(before = v) }
    }

    fun setAfter(v: String) {
        ensureSite()
        _state.update { it.copy(after = v) }
    }

    fun setPublishNow(v: Boolean) = _state.update { it.copy(publishNow = v) }

    fun setCaption(key: Long, caption: MediaCaption) = updateItem(key) { it.copy(caption = caption) }

    /** [stage] = true für per "Teilen" erhaltene Dateien: erst in den App-Speicher kopieren. */
    fun addMedia(uris: List<Uri>, stage: Boolean = false) {
        ensureSite()
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
                        it.copy(state = ItemState.FAILED, error = str(R.string.error_file_unreadable))
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
                _state.value.siteId,
                onWaitingForNetwork = { updateItem(key) { it.copy(state = ItemState.OFFLINE) } },
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
        val account = settings.state.value.let { it.account(s.siteId) ?: it.active } ?: run {
            _state.update { it.copy(publishError = str(R.string.error_credentials_missing_short)) }
            return
        }
        _state.update { it.copy(publishing = true, publishError = null) }
        viewModelScope.launch {
            try {
                val (categoryIds, tagIds) = terms.resolveIds(account.id)
                val media = s.items.mapNotNull { item -> item.uploaded?.let { it to item.caption } }
                val content = BlockBuilder.build(s.before, media, s.after, str(R.string.media_credit_prefix))
                val featured = media.firstOrNull { it.first.isImage }?.first?.id
                val post = WpClient(account.credentials).createPost(
                    s.title.trim(), content, featured,
                    status = if (s.publishNow) "publish" else "draft",
                    categories = categoryIds,
                    tags = tagIds,
                )
                // Entwurf leeren, Ergebnis für die Erfolgsseite behalten.
                _state.value.items.forEach { uploader.releaseStaged(it.uri) }
                terms.reset()
                _state.value = DraftState(published = post)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(publishing = false, publishError = e.message ?: str(R.string.common_unknown_error)) }
            }
        }
    }

    /** Neuer, leerer Entwurf (z. B. nach erfolgreichem Veröffentlichen). */
    fun reset() {
        _state.value.items.forEach { uploader.releaseStaged(it.uri) }
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        terms.reset()
        _state.value = DraftState()
    }

    /** Entwurf verwerfen und bereits hochgeladene Dateien wieder löschen. */
    fun discard() {
        val siteId = _state.value.siteId
        val uploadedIds = _state.value.items.mapNotNull { it.uploaded?.id }
        reset()
        deleteRemote(uploadedIds, siteId)
    }

    private fun deleteRemote(ids: List<Long>, siteId: String? = _state.value.siteId) {
        val account = settings.state.value.let { it.account(siteId) ?: it.active } ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                ids.forEach { id -> runCatching { WpClient(account.credentials).deleteMedia(id) } }
            }
        }
    }

    private fun updateItem(key: Long, transform: (MediaItem) -> MediaItem) = _state.update { s ->
        s.copy(items = s.items.map { if (it.key == key) transform(it) else it })
    }
}
