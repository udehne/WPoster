package de.formerdehne.wposter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.SiteAccount

/** Beim Teilen aus der Galerie mit mehreren Blogs: Ziel-Blog auswählen. */
@Composable
fun SharePickScreen(
    accounts: List<SiteAccount>,
    activeId: String?,
    draftSiteId: String?,
    draftHasContent: Boolean,
    onPick: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by rememberSaveable {
        mutableStateOf(activeId?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id)
    }
    BackHandler(onBack = onCancel)

    Scaffold(topBar = { AppTopBar(stringResource(R.string.sharepick_title), onBack = onCancel) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.sharepick_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                accounts.forEach { account ->
                    BlogChoiceCard(
                        account = account,
                        selected = account.id == selected,
                        onSelect = { selected = account.id },
                    )
                }
            }

            val discardsDraft = draftHasContent && selected != null && selected != draftSiteId
            if (discardsDraft) {
                val draftName = accounts.find { it.id == draftSiteId }?.let { it.siteName.ifBlank { it.host } }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (draftName != null) stringResource(R.string.sharepick_discard_warning, draftName)
                        else stringResource(R.string.sharepick_discard_warning_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(
                onClick = { selected?.let(onPick) },
                enabled = selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.sharepick_continue))
            }
        }
    }
}

@Composable
private fun BlogChoiceCard(account: SiteAccount, selected: Boolean, onSelect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            BrandMark(36.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account.siteName.ifBlank { account.host },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    account.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
