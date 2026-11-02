package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.ui.InfoDoc
import de.formerdehne.wposter.ui.LegalTexts

@Composable
fun LegalScreen(doc: InfoDoc, onBack: () -> Unit) {
    val (title, sections) = when (doc) {
        InfoDoc.HILFE -> stringResource(R.string.common_help) to LegalTexts.help()
        InfoDoc.IMPRESSUM -> stringResource(R.string.legal_title_notice) to LegalTexts.legalNotice()
        InfoDoc.DATENSCHUTZ -> stringResource(R.string.legal_title_privacy) to LegalTexts.privacy()
    }
    Scaffold(topBar = { AppTopBar(title, onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            items(sections) { section ->
                section.heading?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
                    )
                }
                section.body.forEach { paragraph ->
                    Text(
                        paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
