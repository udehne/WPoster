package de.formerdehne.wposter.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.AppSettings
import de.formerdehne.wposter.data.Credentials
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.data.WpClient
import de.formerdehne.wposter.i18n.str
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
                result = true to str(R.string.settings_connected, info.siteName, info.displayName)
                onSaved(!wasConfigured)
            } catch (e: Exception) {
                result = false to (e.message ?: str(R.string.common_unknown_error))
            } finally {
                checking = false
            }
        }
    }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_site))
            OutlinedTextField(
                value = url, onValueChange = { url = it; result = null },
                label = { Text(stringResource(R.string.settings_address)) }, placeholder = { Text(stringResource(R.string.settings_address_placeholder)) },
                singleLine = true,
                trailingIcon = { InfoButton { help = FieldHelp.URL } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it; result = null },
                label = { Text(stringResource(R.string.settings_username)) }, singleLine = true,
                trailingIcon = { InfoButton { help = FieldHelp.USER } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; result = null },
                label = { Text(stringResource(R.string.settings_app_password)) },
                placeholder = { Text("xxxx xxxx xxxx xxxx xxxx xxxx") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) stringResource(R.string.settings_hide) else stringResource(R.string.settings_show))
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
                    Text(stringResource(R.string.settings_checking))
                } else {
                    Text(stringResource(R.string.settings_check_save))
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
                        Text(stringResource(R.string.settings_howto_title), style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        stringResource(R.string.settings_howto_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.settings_stored_encrypted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.settings_section_photos), Modifier.weight(1f))
                InfoButton { help = FieldHelp.IMAGE_SIZE }
            }
            Text(
                stringResource(R.string.settings_photos_text),
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
                    ) { Text(stringResource(R.string.settings_px, edge)) }
                }
            }

            SectionLabel(stringResource(R.string.settings_section_uploads))
            OutlinedTextField(
                value = maxUpload,
                onValueChange = { v ->
                    maxUpload = v.filter { it.isDigit() }.take(4)
                    maxUpload.toIntOrNull()?.let { repository.setMaxUploadMb(it) }
                },
                label = { Text(stringResource(R.string.settings_max_file_size_mb)) },
                trailingIcon = { InfoButton { help = FieldHelp.MAX_UPLOAD } },
                supportingText = { Text(stringResource(R.string.settings_max_file_size_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.settings_section_app))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_check_updates)) },
                    supportingContent = { Text(stringResource(R.string.settings_check_updates_text)) },
                    trailingContent = {
                        Switch(checked = settings.checkUpdates, onCheckedChange = repository::setCheckUpdates)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }

            SectionLabel(stringResource(R.string.settings_section_legal))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LegalLink(stringResource(R.string.legal_title_notice)) { onLegal(InfoDoc.IMPRESSUM) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LegalLink(stringResource(R.string.legal_title_privacy)) { onLegal(InfoDoc.DATENSCHUTZ) }
            }

            if (creds != null) {
                OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_delete_credentials))
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
            title = { Text(stringResource(h.title)) },
            text = { Text(stringResource(h.text)) },
            confirmButton = { TextButton(onClick = { help = null }) { Text(stringResource(R.string.settings_got_it)) } },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.settings_delete_credentials_title)) },
            text = { Text(stringResource(R.string.settings_delete_credentials_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    repository.clearCredentials()
                    password = ""
                    result = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.common_cancel)) } },
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
        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.common_help), tint = MaterialTheme.colorScheme.outline)
    }
}

/** Kurze Hilfetexte zu den Einrichtungsfeldern. */
private enum class FieldHelp(@StringRes val title: Int, @StringRes val text: Int) {
    URL(R.string.settings_help_url_title, R.string.settings_help_url_text),
    USER(R.string.settings_username, R.string.settings_help_user_text),
    PASSWORD(R.string.settings_app_password, R.string.settings_help_password_text),
    IMAGE_SIZE(R.string.settings_help_image_title, R.string.settings_help_image_text),
    MAX_UPLOAD(R.string.settings_help_upload_title, R.string.settings_help_upload_text),
}
