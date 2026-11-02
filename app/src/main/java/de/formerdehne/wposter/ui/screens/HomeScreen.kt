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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import de.formerdehne.wposter.BuildConfig
import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.AppSettings
import de.formerdehne.wposter.data.UpdateChecker
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
    onOpenUrl: (String) -> Unit,
    onDismissUpdate: (String) -> Unit,
    onStartOnboarding: () -> Unit,
    onSwitchBlog: (String) -> Unit,
    onAddBlog: () -> Unit,
    onContinueDraft: () -> Unit,
    draftBlog: String?,
) {
    // Noch kein Blog: Begrüßung statt Beitragsliste.
    if (settings.accounts.isEmpty()) {
        WelcomeScreen(onStart = onStartOnboarding, onDoc = onDoc)
        return
    }
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
                if (settings.accounts.size > 1) {
                    Text(
                        stringResource(R.string.drawer_blogs).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 4.dp),
                    )
                    settings.accounts.forEach { account ->
                        val active = account.id == settings.active?.id
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(account.siteName.ifBlank { account.host }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        account.host,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            icon = {
                                Icon(
                                    if (active) Icons.Default.CheckCircle else Icons.Default.Home,
                                    contentDescription = if (active) stringResource(R.string.drawer_active_blog) else null,
                                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            selected = active,
                            onClick = { closeAnd { if (!active) onSwitchBlog(account.id) } },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                if (configured) {
                    DrawerEntry(stringResource(R.string.drawer_search), Icons.Default.Search) { closeAnd(onSearch) }
                    DrawerEntry(stringResource(R.string.calendar_title), Icons.Default.DateRange) { closeAnd(onCalendar) }
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                DrawerEntry(stringResource(R.string.common_help), Icons.Default.Info) { closeAnd { onDoc(InfoDoc.HILFE) } }
                DrawerEntry(stringResource(R.string.drawer_add_blog), Icons.Default.Add) { closeAnd(onAddBlog) }
                DrawerEntry(stringResource(R.string.settings_title), Icons.Default.Settings) { closeAnd(onSettings) }
                DrawerEntry(stringResource(R.string.legal_title_notice), Icons.Default.AccountBox) { closeAnd { onDoc(InfoDoc.IMPRESSUM) } }
                DrawerEntry(stringResource(R.string.drawer_privacy), Icons.Default.Lock) { closeAnd { onDoc(InfoDoc.DATENSCHUTZ) } }
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
                            Icon(Icons.Default.Menu, stringResource(R.string.home_menu))
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
                        Icon(Icons.Default.Add, stringResource(R.string.common_new_post), Modifier.size(40.dp))
                    }
                }
            },
        ) { padding ->
            if (!configured) {
                WelcomeScreen(onStart = onStartOnboarding, onDoc = onDoc)
            } else {
                val update = settings.latestVersion?.takeIf {
                    settings.checkUpdates && UpdateChecker.enabledForThisBuild &&
                        it != settings.dismissedVersion && UpdateChecker.isNewer(it, BuildConfig.VERSION_NAME)
                }
                PostList(
                    home = home,
                    draft = draft,
                    update = update?.let { it to (settings.latestUrl ?: UpdateChecker.REPO_URL + "/releases/latest") },
                    onOpenUrl = onOpenUrl,
                    onDismissUpdate = onDismissUpdate,
                    padding = padding,
                    onOpenPost = onOpenPost,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    onContinueDraft = onContinueDraft,
                    draftBlog = settings.account(draft.siteId)?.takeIf { settings.accounts.size > 1 }
                        ?.let { it.siteName.ifBlank { it.host } },
                    onDiscardDraft = { confirmDiscard = true },
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.home_discard_draft_title)) },
            text = { Text(stringResource(R.string.home_discard_draft_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscardDraft() }) { Text(stringResource(R.string.common_discard)) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.common_cancel)) } },
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
    update: Pair<String, String>?,
    onOpenUrl: (String) -> Unit,
    onDismissUpdate: (String) -> Unit,
    padding: PaddingValues,
    onOpenPost: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onContinueDraft: () -> Unit,
    draftBlog: String?,
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
            if (update != null) {
                item(key = "update") {
                    UpdateCard(
                        version = update.first,
                        onDownload = { onOpenUrl(update.second) },
                        onDismiss = { onDismissUpdate(update.first) },
                    )
                }
            }
            if (draft.hasContent) {
                item(key = "draft") { DraftCard(draft, draftBlog, onContinueDraft, onDiscardDraft) }
            }
            when {
                home.initialLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                home.posts.isEmpty() && home.error != null -> item(key = "error") {
                    MessageBlock(stringResource(R.string.home_load_failed), home.error, stringResource(R.string.common_retry), onRefresh)
                }
                home.posts.isEmpty() -> item(key = "empty") {
                    MessageBlock(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_text), null, null)
                }
                else -> {
                    items(home.posts, key = { it.id }) { post -> PostCard(post) { onOpenPost(post.id) } }
                    item(key = "footer") {
                        when {
                            home.error != null -> MessageBlock(null, home.error, stringResource(R.string.common_retry), onLoadMore)
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
private fun UpdateCard(version: String, onDownload: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            Text(
                stringResource(R.string.home_update_title, version),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_update_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_update_later)) }
                TextButton(onClick = onDownload) { Text(stringResource(R.string.home_update_download), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun DraftCard(draft: DraftState, blog: String?, onContinue: () -> Unit, onDiscard: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (blog != null) stringResource(R.string.home_draft_for_blog, blog) else stringResource(R.string.home_draft_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                draft.title.ifBlank { stringResource(R.string.home_draft_no_title) } + " · " +
                    if (draft.items.size == 1) stringResource(R.string.home_draft_one_item)
                    else stringResource(R.string.home_draft_items, draft.items.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.common_discard)) }
                TextButton(onClick = onContinue) { Text(stringResource(R.string.home_draft_continue), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
