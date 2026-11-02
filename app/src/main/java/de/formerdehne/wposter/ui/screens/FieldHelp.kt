package de.formerdehne.wposter.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.formerdehne.wposter.R

/** Kurze Hilfetexte zu den Einrichtungsfeldern (Einstellungen, Blog bearbeiten, Einrichtung). */
enum class FieldHelp(@StringRes val title: Int, @StringRes val text: Int) {
    URL(R.string.settings_help_url_title, R.string.settings_help_url_text),
    USER(R.string.settings_username, R.string.settings_help_user_text),
    PASSWORD(R.string.settings_app_password, R.string.settings_help_password_text),
    IMAGE_SIZE(R.string.settings_help_image_title, R.string.settings_help_image_text),
    MAX_UPLOAD(R.string.settings_help_upload_title, R.string.settings_help_upload_text),
}

/** Das kleine ⓘ neben einem Feld. */
@Composable
fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Default.Info,
            contentDescription = stringResource(R.string.common_help),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

/** Dialog mit dem Hilfetext zu [help]. */
@Composable
fun FieldHelpDialog(help: FieldHelp, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text(stringResource(help.title)) },
        text = { Text(stringResource(help.text), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_got_it)) } },
    )
}
