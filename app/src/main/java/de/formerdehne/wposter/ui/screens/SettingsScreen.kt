package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.data.AppSettings
import de.formerdehne.wposter.data.Credentials
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.data.WpClient
import de.formerdehne.wposter.ui.InfoDoc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    onBack: () -> Unit,
    /** firstSetup = true, wenn die App vorher noch nicht eingerichtet war. */
    onSaved: (firstSetup: Boolean) -> Unit,
    onLegal: (InfoDoc) -> Unit,
) {
    val creds = settings.credentials
    val wasConfigured = remember { creds != null }
    var url by rememberSaveable { mutableStateOf(creds?.siteUrl ?: "") }
    var user by rememberSaveable { mutableStateOf(creds?.username ?: "") }
    var password by rememberSaveable { mutableStateOf(creds?.appPassword ?: "") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var maxUpload by rememberSaveable { mutableStateOf(settings.maxUploadMb.toString()) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf<FieldHelp?>(null) }
    val scope = rememberCoroutineScope()

    val canCheck = url.isNotBlank() && user.isNotBlank() && password.isNotBlank() && !checking

    fun check() {
        checking = true
        result = null
        scope.launch {
            try {
                val info = WpClient.probe(url, user, password)
                repository.saveCredentials(
                    Credentials(info.siteUrl, user.trim(), password.trim(), info.useRestRoute),
                    info.siteName,
                    info.siteDescription,
                    info.displayName,
                )
                url = info.siteUrl
                result = true to "Verbunden mit „${info.siteName}“ als ${info.displayName}."
                onSaved(!wasConfigured)
            } catch (e: Exception) {
                result = false to (e.message ?: "Unbekannter Fehler")
            } finally {
                checking = false
            }
        }
    }

    Scaffold(topBar = { AppTopBar("Einstellungen", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("WordPress-Seite")
            OutlinedTextField(
                value = url, onValueChange = { url = it; result = null },
                label = { Text("Adresse") }, placeholder = { Text("https://beispiel.de") },
                singleLine = true,
                trailingIcon = { InfoButton { help = FieldHelp.URL } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it; result = null },
                label = { Text("Benutzername") }, singleLine = true,
                trailingIcon = { InfoButton { help = FieldHelp.USER } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; result = null },
                label = { Text("Anwendungspasswort") },
                placeholder = { Text("xxxx xxxx xxxx xxxx xxxx xxxx") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Verbergen" else "Zeigen")
                        }
                        InfoButton { help = FieldHelp.PASSWORD }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = ::check, enabled = canCheck, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (checking) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Verbindung wird geprüft …")
                } else {
                    Text("Verbindung prüfen & speichern")
                }
            }

            result?.let { (ok, message) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null)
                        Spacer(Modifier.width(12.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("So bekommst du ein Anwendungspasswort", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "1. Im Browser bei WordPress anmelden.\n" +
                            "2. Benutzer → Profil öffnen und nach unten zu „Anwendungspasswörter“ scrollen.\n" +
                            "3. Als Namen z. B. „WPoster Handy“ eingeben und „Neues Anwendungspasswort hinzufügen“ tippen.\n" +
                            "4. Das angezeigte Passwort hier einfügen (Leerzeichen sind egal).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Wird verschlüsselt nur auf diesem Gerät gespeichert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Fotos", Modifier.weight(1f))
                InfoButton { help = FieldHelp.IMAGE_SIZE }
            }
            Text(
                "Maximale Kantenlänge beim Hochladen. Standort und andere Metadaten werden immer entfernt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = SettingsRepository.IMAGE_EDGE_OPTIONS
                options.forEachIndexed { i, edge ->
                    SegmentedButton(
                        selected = settings.maxImageEdge == edge,
                        onClick = { repository.setMaxImageEdge(edge) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text("$edge px") }
                }
            }

            SectionLabel("Uploads")
            OutlinedTextField(
                value = maxUpload,
                onValueChange = { v ->
                    maxUpload = v.filter { it.isDigit() }.take(4)
                    maxUpload.toIntOrNull()?.let { repository.setMaxUploadMb(it) }
                },
                label = { Text("Maximale Dateigröße (MB)") },
                trailingIcon = { InfoButton { help = FieldHelp.MAX_UPLOAD } },
                supportingText = { Text("Siehe WordPress → Medien → Datei hinzufügen.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("App")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text("Nach Updates suchen") },
                    supportingContent = { Text("Fragt gelegentlich bei GitHub nach einer neuen Version.") },
                    trailingContent = {
                        Switch(checked = settings.checkUpdates, onCheckedChange = repository::setCheckUpdates)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }

            SectionLabel("Rechtliches")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LegalLink("Impressum") { onLegal(InfoDoc.IMPRESSUM) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LegalLink("Datenschutzhinweis") { onLegal(InfoDoc.DATENSCHUTZ) }
            }

            if (creds != null) {
                OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Zugangsdaten von diesem Gerät löschen")
                }
            }
            Text(
                "WPoster ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
            )
        }
    }

    help?.let { h ->
        AlertDialog(
            onDismissRequest = { help = null },
            icon = { Icon(Icons.Default.Info, null) },
            title = { Text(h.title) },
            text = { Text(h.text) },
            confirmButton = { TextButton(onClick = { help = null }) { Text("Verstanden") } },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Zugangsdaten löschen?") },
            text = { Text("Das Anwendungspasswort bleibt in WordPress gültig, bis du es dort widerrufst.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    repository.clearCredentials()
                    password = ""
                    result = null
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Info, contentDescription = "Hilfe", tint = MaterialTheme.colorScheme.outline)
    }
}

/** Kurze Hilfetexte zu den Einrichtungsfeldern. */
private enum class FieldHelp(val title: String, val text: String) {
    URL(
        "Adresse der WordPress-Seite",
        "Die Adresse, unter der dein Blog im Browser erreichbar ist, z. B. „https://udehne.de“.\n\n" +
            "Am einfachsten: Blog im Browser öffnen und die Adresse aus der Adresszeile kopieren – " +
            "ohne „/wp-admin“ oder Unterseiten. „https://“ ergänzt die App bei Bedarf selbst."
    ),
    USER(
        "Benutzername",
        "Dein WordPress-Anmeldename – also der Name, mit dem du dich unter „/wp-admin“ anmeldest " +
            "(nicht der angezeigte Name).\n\n" +
            "Zu finden in WordPress unter Benutzer → Profil → „Benutzername“ (ganz oben, grau, nicht änderbar). " +
            "Der Benutzer braucht mindestens die Rolle „Autor“."
    ),
    PASSWORD(
        "Anwendungspasswort",
        "Nicht dein normales WordPress-Passwort, sondern ein eigenes Passwort nur für diese App.\n\n" +
            "So erstellst du es:\n" +
            "1. Im Browser bei WordPress anmelden (…/wp-admin).\n" +
            "2. Links auf Benutzer → Profil.\n" +
            "3. Ganz nach unten zu „Anwendungspasswörter“ scrollen.\n" +
            "4. Namen eingeben, z. B. „WPoster Handy“, und „Neues Anwendungspasswort hinzufügen“ tippen.\n" +
            "5. Das angezeigte Passwort (24 Zeichen in 6 Gruppen) kopieren und hier einfügen. " +
            "Es wird nur einmal angezeigt; Leerzeichen sind egal.\n\n" +
            "Fehlt der Abschnitt, ist die Funktion auf deiner Seite deaktiviert (z. B. durch ein Sicherheits-Plugin). " +
            "Widerrufen kannst du das Passwort jederzeit an derselben Stelle."
    ),
    IMAGE_SIZE(
        "Bildgröße",
        "Fotos werden vor dem Hochladen auf diese maximale Kantenlänge verkleinert. Das spart Zeit, " +
            "mobiles Datenvolumen und Speicherplatz auf dem Server.\n\n" +
            "2560 px (Standard) ist für Blog-Fotos – auch auf großen Bildschirmen – völlig ausreichend. " +
            "Standort (GPS) und andere Metadaten werden in jedem Fall entfernt."
    ),
    MAX_UPLOAD(
        "Maximale Dateigröße",
        "Die größte Datei, die dein Server annimmt. Größere Videos weist die App schon vor dem Hochladen ab.\n\n" +
            "Zu finden in WordPress unter Medien → Datei hinzufügen: Dort steht „Maximale Dateigröße für Uploads: … MB“. " +
            "Diesen Wert hier eintragen."
    ),
}
