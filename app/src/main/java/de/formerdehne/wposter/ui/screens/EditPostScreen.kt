package de.formerdehne.wposter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.ui.EditBlock
import de.formerdehne.wposter.ui.EditState
import de.formerdehne.wposter.ui.TermsEditor
import de.formerdehne.wposter.ui.TermsUi
import de.formerdehne.wposter.data.MediaCaption
import de.formerdehne.wposter.ui.Format
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    state: EditState,
    onLeave: () -> Unit,
    onTitle: (String) -> Unit,
    onDate: (LocalDateTime) -> Unit,
    onText: (Long, String) -> Unit,
    onAddText: (heading: Boolean) -> Unit,
    onAddMedia: (List<android.net.Uri>) -> Unit,
    onMove: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onFeatured: (Long) -> Unit,
    onSave: () -> Unit,
    onReload: () -> Unit,
    onDismissError: () -> Unit,
    terms: TermsUi,
    termsEditor: TermsEditor,
    onRetryTerms: () -> Unit,
    onCaption: (Long, MediaCaption) -> Unit,
    onDraft: (Boolean) -> Unit,
) {
    var confirmLeave by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTimeFor by remember { mutableStateOf<LocalDate?>(null) }

    fun tryLeave() {
        if (state.dirty) confirmLeave = true else onLeave()
    }
    BackHandler(onBack = ::tryLeave)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) onAddMedia(uris)
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.edit_title), onBack = ::tryLeave) },
        bottomBar = {
            if (!state.loading && state.loadError == null) SaveBar(state, onSave)
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.edit_load_failed), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    state.loadError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onReload) { Text(stringResource(R.string.common_retry)) }
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitle,
                    label = { Text(stringResource(R.string.common_title)) },
                    textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                DateCard(state, onClick = { pickDate = true })

                SectionLabel(stringResource(R.string.edit_content))
                if (state.blocks.isEmpty()) {
                    Text(
                        stringResource(R.string.edit_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.blocks.forEachIndexed { index, block ->
                    BlockCard(
                        block = block,
                        isFirst = index == 0,
                        isLast = index == state.blocks.lastIndex,
                        featuredId = state.featuredId,
                        onText = onText,
                        onMove = onMove,
                        onRemove = onRemove,
                        onRetry = onRetry,
                        onFeatured = onFeatured,
                        onCaption = onCaption,
                    )
                }

                SectionLabel(stringResource(R.string.common_add))
                // Untereinander statt in eine Zeile gequetscht.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AddButton(stringResource(R.string.edit_add_text_long)) { onAddText(false) }
                    AddButton(stringResource(R.string.edit_add_heading_long)) { onAddText(true) }
                    AddButton(stringResource(R.string.edit_add_media_long)) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                }

                TermsSection(
                    state = terms,
                    onToggleCategory = termsEditor::toggleCategory,
                    onAddCategory = termsEditor::addCategory,
                    onTagQuery = termsEditor::setTagQuery,
                    onAddTagFromQuery = termsEditor::addTagFromQuery,
                    onAddTag = termsEditor::addTag,
                    onRemoveTag = termsEditor::removeTag,
                    onRetry = onRetryTerms,
                )

                PublishModeCard(publishNow = !state.isDraft, onChange = { onDraft(!it) })
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ---------- Dialoge ----------

    if (pickDate) {
        val initial = (state.date ?: LocalDateTime.now()).toLocalDate()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initial.toEpochDay() * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    pickTimeFor = dateState.selectedDateMillis?.let { LocalDate.ofEpochDay(it / DAY_MS) } ?: initial
                }) { Text(stringResource(R.string.edit_next)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = dateState) }
    }

    pickTimeFor?.let { day ->
        val current = (state.date ?: LocalDateTime.now()).toLocalTime()
        val timeState = rememberTimePickerState(current.hour, current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTimeFor = null },
            title = { Text(stringResource(R.string.edit_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    pickTimeFor = null
                    onDate(LocalDateTime.of(day, LocalTime.of(timeState.hour, timeState.minute)))
                }) { Text(stringResource(R.string.edit_apply)) }
            },
            dismissButton = { TextButton(onClick = { pickTimeFor = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_text)) },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onLeave() }) { Text(stringResource(R.string.common_discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.edit_keep_editing)) } },
        )
    }

    if (state.saving) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.edit_saving)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
        )
    }
    state.saveError?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.edit_save_failed)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismissError) { Text(stringResource(R.string.common_ok)) } },
        )
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun DateCard(state: EditState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.edit_publish_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Format.dateTime(state.date).ifBlank { "–" }, style = MaterialTheme.typography.bodyLarge)
            }
            Text(stringResource(R.string.edit_change), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        val future = state.date?.isAfter(LocalDateTime.now()) == true
        val hint = when {
            future -> stringResource(R.string.edit_date_future)
            state.date != state.originalDate -> stringResource(R.string.edit_date_order)
            else -> null
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (future) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun BlockCard(
    block: EditBlock,
    isFirst: Boolean,
    isLast: Boolean,
    featuredId: Long,
    onText: (Long, String) -> Unit,
    onMove: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onFeatured: (Long) -> Unit,
    onCaption: (Long, MediaCaption) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (block) {
            is EditBlock.Text -> Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = block.text,
                        onValueChange = { onText(block.key, it) },
                        label = { Text(if (block.heading != null) stringResource(R.string.edit_subheading) else stringResource(R.string.common_text)) },
                        textStyle = if (block.heading != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        minLines = if (block.heading != null) 1 else 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (block.hasFormatting) {
                        Text(
                            if (block.text == block.originalText) {
                                stringResource(R.string.edit_formatting_kept)
                            } else {
                                stringResource(R.string.edit_formatting_lost)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Column {
                    MoveButtons(!isFirst, !isLast) { onMove(block.key, it) }
                    IconButton(onClick = { onRemove(block.key) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, stringResource(R.string.common_remove))
                    }
                }
            }
            is EditBlock.Media -> MediaRow(
                thumbnail = block.localUri ?: block.remoteUrl.takeIf { !block.isVideo },
                isVideo = block.isVideo,
                state = block.state,
                progress = block.progress,
                error = block.error,
                canMoveUp = !isFirst,
                canMoveDown = !isLast,
                onMove = { onMove(block.key, it) },
                onRemove = { onRemove(block.key) },
                onRetry = { onRetry(block.key) },
                subtitle = when {
                    block.mediaId != null && block.mediaId == featuredId -> stringResource(R.string.common_featured_image)
                    block.originalRaw != null -> stringResource(R.string.edit_in_post)
                    else -> stringResource(R.string.media_uploaded)
                },
                extraAction = if (!block.isVideo && block.mediaId != null) {
                    @Composable {
                        val isFeatured = block.mediaId == featuredId
                        IconButton(onClick = { block.mediaId?.let(onFeatured) }) {
                            Icon(
                                if (isFeatured) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (isFeatured) stringResource(R.string.common_featured_image) else stringResource(R.string.edit_set_featured),
                                tint = if (isFeatured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                } else null,
                caption = block.caption,
                onCaption = { onCaption(block.key, it) },
            )
            is EditBlock.Other -> Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            block.label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    if (block.summary.isNotBlank()) {
                        Text(
                            block.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.edit_kept_unchanged),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                MoveButtons(!isFirst, !isLast) { onMove(block.key, it) }
                IconButton(onClick = { onRemove(block.key) }) { Icon(Icons.Default.Close, stringResource(R.string.common_remove)) }
            }
        }
    }
}

@Composable
private fun SaveBar(state: EditState, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val hint = when {
                state.waitingForNetwork -> stringResource(R.string.common_hint_offline)
                state.pendingUploads > 0 -> stringResource(R.string.common_hint_pending, state.pendingUploads)
                state.failedUploads > 0 -> stringResource(R.string.common_hint_failed)
                state.title.isBlank() -> stringResource(R.string.common_hint_title)
                else -> null
            }
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
