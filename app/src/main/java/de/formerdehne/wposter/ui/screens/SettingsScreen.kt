package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.AppSettings
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.data.SiteAccount
import de.formerdehne.wposter.ui.InfoDoc

@OptIn(ExperimentalMaterial3Api::class) // SegmentedButton
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onAddBlog: () -> Unit,
    onLegal: (InfoDoc) -> Unit,
) {
    var help by remember { mutableStateOf<FieldHelp?>(null) }
    val activeId = settings.active?.id

    Scaffold(topBar = { AppTopBar(stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- Blogs ----------
            SectionLabel(stringResource(R.string.blogs_section))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (settings.accounts.isEmpty()) {
                    Text(
                        stringResource(R.string.blogs_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                settings.accounts.forEachIndexed { i, account ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BlogRow(
                        account = account,
                        isActive = account.id == activeId,
                        isStart = account.id == settings.startId,
                        onClick = { onOpenAccount(account.id) },
                    )
                }
            }
            OutlinedButton(onClick = onAddBlog, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.blogs_add))
            }

            // ---------- Fotos ----------
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

            // ---------- App ----------
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

            // ---------- Rechtliches ----------
            SectionLabel(stringResource(R.string.settings_section_legal))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LegalLink(stringResource(R.string.legal_title_notice)) { onLegal(InfoDoc.IMPRESSUM) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LegalLink(stringResource(R.string.legal_title_privacy)) { onLegal(InfoDoc.DATENSCHUTZ) }
            }

            Text(
                "WPoster ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
            )
        }
    }

    help?.let { h -> FieldHelpDialog(h) { help = null } }
}

@Composable
private fun BlogRow(account: SiteAccount, isActive: Boolean, isStart: Boolean, onClick: () -> Unit) {
    val title = account.siteName.ifBlank { account.host }
    val supporting = if (account.displayName.isBlank()) account.host else "${account.host} · ${account.displayName}"
    ListItem(
        leadingContent = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (isActive) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.blogs_active),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(supporting, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStart) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            stringResource(R.string.blogs_start_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
