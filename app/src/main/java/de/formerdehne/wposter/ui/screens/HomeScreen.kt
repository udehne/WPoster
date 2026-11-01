package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.data.AppSettings
import de.formerdehne.wposter.data.PostSummary
import de.formerdehne.wposter.ui.DraftState
import de.formerdehne.wposter.ui.Format
import de.formerdehne.wposter.ui.HomeState
import de.formerdehne.wposter.ui.InfoDoc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    home: HomeState,
    draft: DraftState,
    onCompose: () -> Unit,
    onDiscardDraft: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSettings: () -> Unit,
    onDoc: (InfoDoc) -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var confirmDiscard by remember { mutableStateOf(false) }
    val configured = settings.credentials != null
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    fun closeAnd(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(24.dp)) {
                    BrandMark(56.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(settings.siteName ?: "WPoster", style = MaterialTheme.typography.titleLarge)
                    settings.credentials?.let {
                        Text(
                            it.siteUrl.removePrefix("https://"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                if (configured) {
                    DrawerEntry("Suche", Icons.Default.Search) { closeAnd(onSearch) }
                    DrawerEntry("Kalender", Icons.Default.DateRange) { closeAnd(onCalendar) }
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                DrawerEntry("Hilfe", Icons.Default.Info) { closeAnd { onDoc(InfoDoc.HILFE) } }
                DrawerEntry("Einstellungen", Icons.Default.Settings) { closeAnd(onSettings) }
                DrawerEntry("Impressum", Icons.Default.AccountBox) { closeAnd { onDoc(InfoDoc.IMPRESSUM) } }
                DrawerEntry("Datenschutz", Icons.Default.Lock) { closeAnd { onDoc(InfoDoc.DATENSCHUTZ) } }
                Spacer(Modifier.weight(1f))
                Text(
                    "WPoster ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(24.dp),
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (configured) settings.siteName ?: "WPoster" else "WPoster",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val subtitle = if (configured) settings.siteDescription else null
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menü")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                if (configured) {
                    LargeFloatingActionButton(
                        onClick = onCompose,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Default.Add, "Neuer Beitrag", Modifier.size(40.dp))
                    }
                }
            },
        ) { padding ->
            if (!configured) {
                Welcome(Modifier.padding(padding), onSettings)
            } else {
                PostList(
                    home = home,
                    draft = draft,
                    padding = padding,
                    onOpenPost = onOpenPost,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    onContinueDraft = onCompose,
                    onDiscardDraft = { confirmDiscard = true },
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
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

@Composable
private fun DrawerEntry(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostList(
    home: HomeState,
    draft: DraftState,
    padding: PaddingValues,
    onOpenPost: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onContinueDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = home.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (draft.hasContent) {
                item(key = "draft") { DraftCard(draft, onContinueDraft, onDiscardDraft) }
            }
            when {
                home.initialLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                home.posts.isEmpty() && home.error != null -> item(key = "error") {
                    MessageBlock("Beiträge konnten nicht geladen werden", home.error, "Erneut versuchen", onRefresh)
                }
                home.posts.isEmpty() -> item(key = "empty") {
                    MessageBlock("Noch keine Beiträge", "Tippe auf + und veröffentliche deinen ersten Beitrag.", null, null)
                }
                else -> {
                    items(home.posts, key = { it.id }) { post -> PostCard(post) { onOpenPost(post.id) } }
                    item(key = "footer") {
                        when {
                            home.error != null -> MessageBlock(null, home.error, "Erneut versuchen", onLoadMore)
                            !home.endReached -> {
                                LaunchedEffect(home.posts.size) { onLoadMore() }
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCard(draft: DraftState, onContinue: () -> Unit, onDiscard: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text("Entwurf in Bearbeitung", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                draft.title.ifBlank { "Ohne Überschrift" } + " · " +
                    if (draft.items.size == 1) "1 Medium" else "${draft.items.size} Medien",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDiscard) { Text("Verwerfen") }
                TextButton(onClick = onContinue) { Text("Fortsetzen", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Welcome(modifier: Modifier, onSettings: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        BrandMark(88.dp)
        Spacer(Modifier.height(20.dp))
        Text("Willkommen bei WPoster", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Verbinde die App mit deinem WordPress-Blog, um Beiträge zu sehen, zu schreiben und zu bearbeiten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.size(10.dp))
            Text("Jetzt einrichten")
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
                    "Fotos und Videos in der Galerie auswählen, auf „Teilen“ tippen und WPoster wählen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
