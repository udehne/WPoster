package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.MediaCaption
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import de.formerdehne.wposter.ui.ItemState

/** Eine Zeile für ein Foto/Video mit Vorschau, Upload-Status und Verschieben/Entfernen. */
@Composable
fun MediaRow(
    thumbnail: Any?,
    isVideo: Boolean,
    state: ItemState,
    progress: Float,
    error: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    subtitle: String? = null,
    extraAction: (@Composable () -> Unit)? = null,
    caption: MediaCaption? = null,
    onCaption: ((MediaCaption) -> Unit)? = null,
) {
  Column(Modifier.fillMaxWidth()) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isVideo) stringResource(R.string.media_video) else stringResource(R.string.media_photo), style = MaterialTheme.typography.titleSmall)
            when (state) {
                ItemState.WAITING -> StatusText(stringResource(R.string.media_waiting))
                ItemState.OFFLINE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    StatusText(stringResource(R.string.media_waiting_network))
                }
                ItemState.PREPARING -> {
                    StatusText(if (isVideo) stringResource(R.string.media_cleaning) else stringResource(R.string.media_resizing))
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                ItemState.UPLOADING -> {
                    StatusText(stringResource(R.string.media_uploading, (progress * 100).toInt()))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                ItemState.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    StatusText(subtitle ?: stringResource(R.string.media_uploaded))
                }
                ItemState.FAILED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            error ?: stringResource(R.string.media_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
        }
        extraAction?.invoke()
        MoveButtons(canMoveUp, canMoveDown, onMove)
        IconButton(onClick = onRemove) { Icon(Icons.Default.Close, stringResource(R.string.common_remove)) }
    }
    if (caption != null && onCaption != null) {
        CaptionEditor(caption, onCaption)
    }
  }
}

/** Optionale Bildunterschrift + Credit, aufklappbar unter der Medienzeile. */
@Composable
private fun CaptionEditor(caption: MediaCaption, onCaption: (MediaCaption) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    if (!open) {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.padding(start = 84.dp, bottom = 4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            if (caption.isEmpty) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.caption_add))
            } else {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    listOf(caption.caption.trim(), caption.credit.trim().takeIf { it.isNotEmpty() }?.let { "© $it" })
                        .filter { !it.isNullOrBlank() }.joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        OutlinedTextField(
            value = caption.caption,
            onValueChange = { onCaption(caption.copy(caption = it)) },
            label = { Text(stringResource(R.string.caption_label)) },
            placeholder = { Text(stringResource(R.string.caption_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = caption.credit,
            onValueChange = { onCaption(caption.copy(credit = it)) },
            label = { Text(stringResource(R.string.caption_credit_label)) },
            placeholder = { Text(stringResource(R.string.caption_credit_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { open = false }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.caption_done))
        }
    }
}

@Composable
fun MoveButtons(canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Int) -> Unit) {
    Column {
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.media_move_up))
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.media_move_down))
        }
    }
}

@Composable
fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
