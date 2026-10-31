package de.formerdehne.wordprest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.formerdehne.wordprest.data.AppSettings
import de.formerdehne.wordprest.ui.DraftState
import de.formerdehne.wordprest.ui.LegalDoc

@Composable
fun HomeScreen(
    settings: AppSettings,
    draft: DraftState,
    onCompose: () -> Unit,
    onDiscardDraft: () -> Unit,
    onSettings: () -> Unit,
    onLegal: (LegalDoc) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val configured = settings.credentials != null

    Scaffold(
        topBar = {
            AppTopBar(title = "", actions = {
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Einstellungen") }
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Mehr") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Impressum") }, onClick = { menuOpen = false; onLegal(LegalDoc.IMPRESSUM) })
                    DropdownMenuItem(text = { Text("Datenschutz") }, onClick = { menuOpen = false; onLegal(LegalDoc.DATENSCHUTZ) })
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            BrandMark(88.dp)
            Spacer(Modifier.height(20.dp))
            Text("WordPREST", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                if (configured) settings.siteName ?: settings.credentials!!.siteUrl else "Noch nicht eingerichtet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (configured) {
                Text(
                    "${settings.credentials!!.siteUrl.removePrefix("https://")} · ${settings.displayName ?: settings.credentials!!.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(40.dp))

            if (!configured) {
                Button(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(Modifier.size(10.dp))
                    Text("Jetzt einrichten")
                }
            } else {
                Button(onClick = onCompose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(if (draft.hasContent) Icons.Default.Edit else Icons.Default.Add, null)
                    Spacer(Modifier.size(10.dp))
                    Text(if (draft.hasContent) "Entwurf fortsetzen" else "Neuer Beitrag")
                }
                if (draft.hasContent) {
                    TextButton(onClick = { confirmDiscard = true }) { Text("Entwurf verwerfen") }
                }
            }

            Spacer(Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Direkt aus der Galerie", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fotos und Videos in der Galerie auswählen, auf „Teilen“ tippen und WordPREST wählen. " +
                            "Der Upload startet sofort – Überschrift und Texte ergänzt du währenddessen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDiscard) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Entwurf verwerfen?") },
            text = { Text("Bereits hochgeladene Fotos und Videos dieses Entwurfs werden wieder von der Seite gelöscht.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscardDraft() }) { Text("Verwerfen") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmDiscard = false }) { Text("Abbrechen") } },
        )
    }
}
