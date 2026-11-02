package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.Credentials
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.data.SiteAccount
import de.formerdehne.wposter.data.WpClient
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Einen eingerichteten Blog bearbeiten oder entfernen. */
@Composable
fun AccountScreen(
    account: SiteAccount,
    isStart: Boolean,
    isOnlyAccount: Boolean,
    repository: SettingsRepository,
    onBack: () -> Unit,
    onRemoved: () -> Unit,
) {
    val creds = account.credentials
    var url by rememberSaveable(account.id) { mutableStateOf(creds.siteUrl) }
    var user by rememberSaveable(account.id) { mutableStateOf(creds.username) }
    var password by rememberSaveable(account.id) { mutableStateOf(creds.appPassword) }
    var showPassword by rememberSaveable(account.id) { mutableStateOf(false) }
    var maxUpload by rememberSaveable(account.id) { mutableStateOf(account.maxUploadMb.toString()) }
    var checking by remember(account.id) { mutableStateOf(false) }
    var result by remember(account.id) { mutableStateOf<Pair<Boolean, String>?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf<FieldHelp?>(null) }
    val scope = rememberCoroutineScope()

    val title = account.siteName.ifBlank { account.host }
    val canCheck = url.isNotBlank() && user.isNotBlank() && password.isNotBlank() && !checking

    fun check() {
        checking = true
        result = null
        scope.launch {
            try {
                val info = WpClient.probe(url, user, password)
                repository.saveAccount(
                    account.id,
                    Credentials(info.siteUrl, user.trim(), password.trim(), info.useRestRoute),
                    info.siteName,
                    info.siteDescription,
                    info.displayName,
                )
                url = info.siteUrl
                result = true to str(R.string.settings_connected, info.siteName, info.displayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                result = false to (e.message ?: str(R.string.common_unknown_error))
            } finally {
                checking = false
            }
        }
    }

    Scaffold(topBar = { AppTopBar(title, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(stringResource(R.string.account_section_access))
            OutlinedTextField(
                value = url, onValueChange = { url = it; result = null },
                label = { Text(stringResource(R.string.settings_address)) },
                placeholder = { Text(stringResource(R.string.settings_address_placeholder)) },
                singleLine = true,
                trailingIcon = { InfoButton { help = FieldHelp.URL } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it; result = null },
                label = { Text(stringResource(R.string.settings_username)) },
                singleLine = true,
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
                            Text(stringResource(if (showPassword) R.string.settings_hide else R.string.settings_show))
                        }
                        InfoButton { help = FieldHelp.PASSWORD }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = ::check, enabled = canCheck, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (checking) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.settings_checking))
                } else {
                    Text(stringResource(R.string.settings_check_save))
                }
            }

            result?.let { (ok, message) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_stored_encrypted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            SectionLabel(stringResource(R.string.settings_section_uploads))
            OutlinedTextField(
                value = maxUpload,
                onValueChange = { v ->
                    maxUpload = v.filter { it.isDigit() }.take(4)
                    maxUpload.toIntOrNull()?.let { repository.setMaxUploadMb(account.id, it) }
                },
                label = { Text(stringResource(R.string.settings_max_file_size_mb)) },
                trailingIcon = { InfoButton { help = FieldHelp.MAX_UPLOAD } },
                supportingText = { Text(stringResource(R.string.settings_max_file_size_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.account_section_start))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.account_show_on_start)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (isOnlyAccount) R.string.account_only_blog_hint else R.string.account_show_on_start_text
                            )
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isStart,
                            onCheckedChange = { checked -> if (checked) repository.setStart(account.id) },
                            enabled = !isStart,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmRemove = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.account_remove))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    help?.let { h -> FieldHelpDialog(h) { help = null } }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text(stringResource(R.string.account_remove_title, title)) },
            text = { Text(stringResource(R.string.account_remove_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        repository.removeAccount(account.id)
                        onRemoved()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}
