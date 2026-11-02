package de.formerdehne.wposter.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.R
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.BlockBuilder
import de.formerdehne.wposter.data.BlockParser
import de.formerdehne.wposter.data.MediaCaption
import de.formerdehne.wposter.data.MediaUploader
import de.formerdehne.wposter.data.ParsedBlock
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Ein Inhaltsblock im Bearbeiten-Editor. */
sealed interface EditBlock {
    val key: Long

    /** Absatz (heading == null) oder Zwischenüberschrift (heading = Ebene). */
    data class Text(
        override val key: Long,
        val text: String,
        val heading: Int? = null,
        val originalText: String? = null,
        val originalRaw: String? = null,
        val hasFormatting: Boolean = false,
    ) : EditBlock

    data class Media(
        override val key: Long,
        val isVideo: Boolean,
        /** Lokale Datei (neu hinzugefügt) … */
        val localUri: Uri? = null,
        /** … oder Adresse auf dem Server (bereits im Beitrag). */
        val remoteUrl: String? = null,
        val state: ItemState = ItemState.DONE,
        val progress: Float = 0f,
        val error: String? = null,
        val uploaded: UploadedMedia? = null,
        val originalRaw: String? = null,
        val caption: MediaCaption = MediaCaption(),
        /** Bildunterschrift beim Laden – nur bei Änderung wird der Block angepasst. */
        val originalCaption: MediaCaption? = null,
    ) : EditBlock {
        val mediaId: Long? get() = uploaded?.id?.takeIf { it > 0 }
    }

    /** Inhalt, den die App nicht bearbeiten kann – bleibt unverändert erhalten. */
    data class Other(override val key: Long, val label: String, val summary: String, val raw: String) : EditBlock
}

data class EditState(
    val postId: Long = 0,
    /** Blog, zu dem der Beitrag gehört. */
    val siteId: String? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val title: String = "",
    val date: LocalDateTime? = null,
    val originalDate: LocalDateTime? = null,
    val status: String = "publish",
    /** true = als Entwurf speichern (nicht öffentlich). */
    val isDraft: Boolean = false,
    val featuredId: Long = 0,
    val blocks: List<EditBlock> = emptyList(),
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Wird nach erfolgreichem Speichern gesetzt (einmaliges Ereignis). */
    val savedId: Long? = null,
) {
    private val media get() = blocks.filterIsInstance<EditBlock.Media>()
    val pendingUploads get() = media.count { it.state != ItemState.DONE && it.state != ItemState.FAILED }
    val failedUploads get() = media.count { it.state == ItemState.FAILED }
    val waitingForNetwork get() = media.any { it.state == ItemState.OFFLINE }
    val canSave get() = !loading && loadError == null && title.isNotBlank() && !saving &&
        pendingUploads == 0 && failedUploads == 0
}

class EditViewModel(app: Application) : AndroidViewModel(app) {

    private val wposter = app as WPosterApp
    private val settings = wposter.settings
    private val uploader = MediaUploader(app, settings, wposter.network)
    private val jobs = HashMap<Long, Job>()
    private var nextKey = 1L
    private var originalImageIds: Set<Long> = emptySet()
    private var loadJob: Job? = null

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    /** Kategorien & Schlagwörter des Beitrags; Änderungen markieren den Beitrag als geändert. */
    val terms = TermsEditor(viewModelScope, wposter.terms) {
        _state.update { it.copy(dirty = true, saveError = null) }
    }

    /** Editor für einen Beitrag öffnen. Ungespeicherte Änderungen am selben Beitrag bleiben erhalten. */
    fun start(id: Long) {
        val s = _state.value
        if (s.postId == id && (s.loading || s.dirty || s.blocks.isNotEmpty()) && s.loadError == null && s.savedId == null) return
        load(id)
    }

    fun load(id: Long) {
        val account = settings.state.value.active ?: return
        cancelUploads()
        loadJob?.cancel()
        terms.reset()
        _state.value = EditState(postId = id, siteId = account.id, loading = true)
        loadJob = viewModelScope.launch {
            try {
                val post = WpClient(account.credentials).getPostForEdit(id)
                val blocks = BlockParser.parse(post.content).map { it.toEditBlock() }
                originalImageIds = blocks.filterIsInstance<EditBlock.Media>()
                    .filter { !it.isVideo }.mapNotNull { it.mediaId }.toSet()
                terms.load(account.id, post.categories, post.tags)
                _state.value = EditState(
                    postId = id,
                    siteId = account.id,
                    isDraft = post.status == "draft",
                    title = post.title,
                    date = post.date,
                    originalDate = post.date,
                    status = post.status,
                    featuredId = post.featuredMedia,
                    blocks = blocks,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = EditState(postId = id, siteId = account.id, loadError = e.message ?: str(R.string.common_unknown_error))
            }
        }
    }

    private fun ParsedBlock.toEditBlock(): EditBlock = when (this) {
        is ParsedBlock.Paragraph -> EditBlock.Text(nextKey++, text, null, text, raw, hasFormatting)
        is ParsedBlock.Heading -> EditBlock.Text(nextKey++, text, level, text, raw, hasFormatting)
        is ParsedBlock.Image -> EditBlock.Media(
            nextKey++, isVideo = false, remoteUrl = src, originalRaw = raw,
            uploaded = UploadedMedia(id, src.orEmpty(), null, isImage = true, isVideo = false),
            caption = MediaCaption(caption, credit), originalCaption = MediaCaption(caption, credit),
        )
        is ParsedBlock.Video -> EditBlock.Media(
            nextKey++, isVideo = true, remoteUrl = src, originalRaw = raw,
            uploaded = UploadedMedia(id, src.orEmpty(), null, isImage = false, isVideo = true),
            caption = MediaCaption(caption, credit), originalCaption = MediaCaption(caption, credit),
        )
        is ParsedBlock.Other -> EditBlock.Other(
            nextKey++,
            otherLabel(blockName),
            if (blockName == "gallery") {
                if (imageCount == 1) str(R.string.edit_gallery_one_image) else str(R.string.edit_gallery_images, imageCount)
            } else summary,
            raw,
        )
    }

    private fun otherLabel(name: String): String = when (name) {
        ParsedBlock.Other.FREEFORM -> str(R.string.edit_block_classic)
        "gallery" -> str(R.string.edit_block_gallery)
        "list" -> str(R.string.edit_block_list)
        "quote" -> str(R.string.edit_block_quote)
        "embed" -> str(R.string.edit_block_embed)
        "columns" -> str(R.string.edit_block_columns)
        "group" -> str(R.string.edit_block_group)
        "cover" -> str(R.string.edit_block_cover)
        "separator" -> str(R.string.edit_block_separator)
        "spacer" -> str(R.string.edit_block_spacer)
        "audio" -> str(R.string.edit_block_audio)
        "file" -> str(R.string.edit_block_file)
        "media-text" -> str(R.string.edit_block_media_text)
        "html" -> str(R.string.edit_block_html)
        else -> str(R.string.edit_block_generic, name)
    }

    // ---------- Bearbeiten ----------

    private fun edit(transform: (EditState) -> EditState) =
        _state.update { transform(it).copy(dirty = true, saveError = null) }

    fun setTitle(v: String) = edit { it.copy(title = v) }

    fun setDate(v: LocalDateTime) = edit { it.copy(date = v) }

    fun setDraft(v: Boolean) = edit { it.copy(isDraft = v) }

    fun setCaption(key: Long, caption: MediaCaption) = edit { s ->
        s.copy(blocks = s.blocks.map { if (it.key == key && it is EditBlock.Media) it.copy(caption = caption) else it })
    }

    fun setText(key: Long, text: String) = edit { s ->
        s.copy(blocks = s.blocks.map { if (it.key == key && it is EditBlock.Text) it.copy(text = text) else it })
    }

    fun addText(heading: Boolean) = edit { s ->
        s.copy(blocks = s.blocks + EditBlock.Text(nextKey++, "", if (heading) 2 else null))
    }

    fun setFeatured(mediaId: Long) = edit { it.copy(featuredId = mediaId) }

    fun move(key: Long, delta: Int) = edit { s ->
        val list = s.blocks.toMutableList()
        val from = list.indexOfFirst { it.key == key }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@edit s
        list.add(to, list.removeAt(from))
        s.copy(blocks = list)
    }

    fun remove(key: Long) {
        jobs.remove(key)?.cancel()
        val block = _state.value.blocks.find { it.key == key } ?: return
        edit { s -> s.copy(blocks = s.blocks.filterNot { it.key == key }) }
        // In dieser Sitzung hochgeladene Dateien wieder löschen; bestehende bleiben in der Mediathek.
        if (block is EditBlock.Media && block.originalRaw == null) block.mediaId?.let { deleteRemote(listOf(it)) }
    }

    fun addMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val resolver = getApplication<Application>().contentResolver
        val newBlocks = uris.map { uri ->
            EditBlock.Media(
                key = nextKey++,
                isVideo = resolver.getType(uri)?.startsWith("video/") == true,
                localUri = uri,
                state = ItemState.WAITING,
            )
        }
        edit { s ->
            // Hinter dem letzten vorhandenen Foto/Video einfügen, sonst ans Ende.
            val lastMedia = s.blocks.indexOfLast { it is EditBlock.Media }
            val list = s.blocks.toMutableList()
            list.addAll(if (lastMedia >= 0) lastMedia + 1 else list.size, newBlocks)
            s.copy(blocks = list)
        }
        newBlocks.forEach { startUpload(it.key) }
    }

    fun retry(key: Long) = startUpload(key)

    private fun startUpload(key: Long) {
        jobs[key]?.cancel()
        updateMedia(key) { it.copy(state = ItemState.WAITING, error = null, progress = 0f) }
        jobs[key] = viewModelScope.launch {
            uploader.lock.withLock { upload(key) }
        }
    }

    private suspend fun upload(key: Long) {
        val block = _state.value.blocks.find { it.key == key } as? EditBlock.Media ?: return
        val uri = block.localUri ?: return
        try {
            var lastPercent = -1
            val uploaded = uploader.upload(
                uri,
                _state.value.siteId,
                onWaitingForNetwork = { updateMedia(key) { it.copy(state = ItemState.OFFLINE) } },
                onPreparing = { updateMedia(key) { it.copy(state = ItemState.PREPARING) } },
                onUploading = { updateMedia(key) { it.copy(state = ItemState.UPLOADING, progress = 0f) } },
                onProgress = { p ->
                    val percent = (p * 100).toInt()
                    if (percent != lastPercent) {
                        lastPercent = percent
                        updateMedia(key) { it.copy(progress = p) }
                    }
                },
            )
            updateMedia(key) { it.copy(state = ItemState.DONE, progress = 1f, uploaded = uploaded) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateMedia(key) { it.copy(state = ItemState.FAILED, error = e.message ?: e.javaClass.simpleName) }
        }
    }

    private fun updateMedia(key: Long, transform: (EditBlock.Media) -> EditBlock.Media) = _state.update { s ->
        s.copy(blocks = s.blocks.map { if (it.key == key && it is EditBlock.Media) transform(it) else it })
    }

    // ---------- Speichern / Verwerfen ----------

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val account = settings.state.value.let { it.account(s.siteId) ?: it.active } ?: return
        val creditPrefix = str(R.string.media_credit_prefix)
        val content = s.blocks.mapNotNull { block ->
            when (block) {
                is EditBlock.Text -> when {
                    block.originalRaw != null && block.text == block.originalText -> block.originalRaw
                    block.text.isBlank() -> null
                    block.heading != null -> BlockBuilder.heading(block.text, block.heading)
                    else -> BlockBuilder.paragraphs(block.text).joinToString("\n\n").ifBlank { null }
                }
                is EditBlock.Media -> when {
                    block.originalRaw != null && block.caption == block.originalCaption -> block.originalRaw
                    block.originalRaw != null ->
                        BlockBuilder.replaceCaption(block.originalRaw, BlockBuilder.captionHtml(block.caption, creditPrefix))
                    else -> block.uploaded?.let { BlockBuilder.media(it, block.caption, creditPrefix) }
                }
                is EditBlock.Other -> block.raw
            }
        }.joinToString("\n\n")

        val imageIds = s.blocks.filterIsInstance<EditBlock.Media>().filter { !it.isVideo }.mapNotNull { it.mediaId }
        var featured = s.featuredId
        // Beitragsbild wurde aus dem Beitrag entfernt → erstes verbleibendes Foto nehmen.
        if (featured != 0L && featured !in imageIds && featured in originalImageIds) featured = 0L
        if (featured == 0L) featured = imageIds.firstOrNull() ?: 0L

        val dateParam = s.date?.takeIf { it != s.originalDate }?.format(WP_DATE)
        // Status nur senden, wenn er sich ändern soll (geplante Beiträge bleiben geplant).
        val statusParam = when {
            s.isDraft && s.status != "draft" -> "draft"
            !s.isDraft && s.status == "draft" -> "publish"
            else -> null
        }

        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val (categoryIds, tagIds) = terms.resolveIds(account.id)
                WpClient(account.credentials).updatePost(
                    s.postId, s.title.trim(), content, dateParam, featured,
                    status = statusParam, categories = categoryIds, tags = tagIds,
                )
                _state.update { it.copy(saving = false, dirty = false, savedId = s.postId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, saveError = e.message ?: str(R.string.common_unknown_error)) }
            }
        }
    }

    fun consumeSaved() {
        jobs.clear()
        terms.reset()
        _state.value = EditState()
    }

    fun dismissError() = _state.update { it.copy(saveError = null) }

    /** Änderungen verwerfen; in dieser Sitzung hochgeladene Dateien wieder löschen. */
    fun discard() {
        val sessionUploads = _state.value.blocks.filterIsInstance<EditBlock.Media>()
            .filter { it.originalRaw == null }.mapNotNull { it.mediaId }
        val siteId = _state.value.siteId
        cancelUploads()
        terms.reset()
        _state.value = EditState()
        deleteRemote(sessionUploads, siteId)
    }

    private fun cancelUploads() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
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

    companion object {
        private val WP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}
