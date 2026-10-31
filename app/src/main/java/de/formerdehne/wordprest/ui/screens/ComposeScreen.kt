package de.formerdehne.wordprest.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.formerdehne.wordprest.ui.DraftState
import de.formerdehne.wordprest.ui.ItemState
import de.formerdehne.wordprest.ui.MediaItem

@Composable
fun ComposeScreen(
    state: DraftState,
    onBack: () -> Unit,
    onTitle: (String) -> Unit,
    onBefore: (String) -> Unit,
    onAfter: (String) -> Unit,
    onAddMedia: (List<android.net.Uri>) -> Unit,
    onRemove: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onPublish: () -> Unit,
    onDismissError: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) onAddMedia(uris)
    }
    val pickMedia = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    Scaffold(
        topBar = { AppTopBar(title = "Neuer Beitrag", onBack = onBack) },
        bottomBar = { PublishBar(state, onPublish) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitle,
                label = { Text("Überschrift") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.before,
                onValueChange = onBefore,
                label = { Text("Text vor den Fotos (optional)") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Fotos & Videos (${state.items.size})", Modifier.weight(1f))
                TextButton(onClick = pickMedia) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hinzufügen")
                }
            }

            if (state.items.isEmpty()) {
                Card(
                    onClick = pickMedia,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Fotos oder Videos auswählen", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Sie werden sofort hochgeladen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    state.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        MediaRow(
                            item = item,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.items.lastIndex,
                            onMove = { onMove(item.key, it) },
                            onRemove = { onRemove(item.key) },
                            onRetry = { onRetry(item.key) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.after,
                onValueChange = onAfter,
                label = { Text("Text nach den Fotos (optional)") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (state.publishing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Wird veröffentlicht …") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            },
        )
    }
    state.publishError?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Veröffentlichen fehlgeschlagen") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun MediaRow(
    item: MediaItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.isVideo) {
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
            Text(
                if (item.isVideo) "Video" else "Foto",
                style = MaterialTheme.typography.titleSmall,
            )
            when (item.state) {
                ItemState.WAITING -> StatusText("Wartet …")
                ItemState.PREPARING -> {
                    StatusText(if (item.isVideo) "Wird bereinigt …" else "Wird verkleinert …")
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                ItemState.UPLOADING -> {
                    StatusText("Lädt hoch … ${(item.progress * 100).toInt()} %")
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                ItemState.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    StatusText("Hochgeladen")
                }
                ItemState.FAILED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            item.error ?: "Fehler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Erneut versuchen")
                    }
                }
            }
        }
        Column {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Nach oben")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Nach unten")
            }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Entfernen") }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PublishBar(state: DraftState, onPublish: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val hint = when {
                state.pendingUploads > 0 -> "Noch ${state.pendingUploads} Upload(s) laufen …"
                state.failedUploads > 0 -> "Bitte fehlgeschlagene Uploads wiederholen oder entfernen."
                state.title.isBlank() -> "Bitte eine Überschrift eingeben."
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
                onClick = onPublish,
                enabled = state.canPublish,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Veröffentlichen", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

