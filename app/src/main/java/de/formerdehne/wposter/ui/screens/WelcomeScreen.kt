package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.ui.InfoDoc

/** Begrüßung beim ersten Start, solange noch kein Blog eingerichtet ist. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, onDoc: (InfoDoc) -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            BrandMark(96.dp)
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.welcome_pitch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    WelcomeFeature(
                        Icons.Default.Share,
                        stringResource(R.string.welcome_feature_share_title),
                        stringResource(R.string.welcome_feature_share_text),
                    )
                    WelcomeFeature(
                        Icons.Default.DateRange,
                        stringResource(R.string.welcome_feature_browse_title),
                        stringResource(R.string.welcome_feature_browse_text),
                    )
                    WelcomeFeature(
                        Icons.Default.Edit,
                        stringResource(R.string.welcome_feature_edit_title),
                        stringResource(R.string.welcome_feature_edit_text),
                    )
                    WelcomeFeature(
                        Icons.Default.Home,
                        stringResource(R.string.welcome_feature_blogs_title),
                        stringResource(R.string.welcome_feature_blogs_text),
                    )
                    WelcomeFeature(
                        Icons.Default.Lock,
                        stringResource(R.string.welcome_feature_privacy_title),
                        stringResource(R.string.welcome_feature_privacy_text),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onDoc(InfoDoc.DATENSCHUTZ) }) {
                    Text(stringResource(R.string.legal_title_privacy), style = MaterialTheme.typography.bodySmall)
                }
                Text("·", color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = { onDoc(InfoDoc.IMPRESSUM) }) {
                    Text(stringResource(R.string.legal_title_notice), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
