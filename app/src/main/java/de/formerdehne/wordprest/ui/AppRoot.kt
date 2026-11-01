package de.formerdehne.wordprest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.formerdehne.wordprest.data.SettingsRepository
import de.formerdehne.wordprest.ui.screens.CalendarScreen
import de.formerdehne.wordprest.ui.screens.ComposeScreen
import de.formerdehne.wordprest.ui.screens.SearchScreen
import de.formerdehne.wordprest.ui.screens.EditPostScreen
import de.formerdehne.wordprest.ui.screens.HomeScreen
import de.formerdehne.wordprest.ui.screens.LegalScreen
import de.formerdehne.wordprest.ui.screens.PostScreen
import de.formerdehne.wordprest.ui.screens.SettingsScreen
import de.formerdehne.wordprest.ui.screens.SuccessScreen

@Composable
fun AppRoot(
    nav: NavViewModel,
    draftVm: ComposeViewModel,
    homeVm: HomeViewModel,
    postVm: PostViewModel,
    editVm: EditViewModel,
    searchVm: SearchViewModel,
    calendarVm: CalendarViewModel,
    repository: SettingsRepository,
) {
    val settings by repository.state.collectAsStateWithLifecycle()
    val draft by draftVm.state.collectAsStateWithLifecycle()
    val home by homeVm.state.collectAsStateWithLifecycle()
    val post by postVm.state.collectAsStateWithLifecycle()
    val edit by editVm.state.collectAsStateWithLifecycle()
    val search by searchVm.state.collectAsStateWithLifecycle()
    val calendar by calendarVm.state.collectAsStateWithLifecycle()
    val screen = nav.current

    // Nach erfolgreichem Veröffentlichen: Erfolgsseite, Liste aktualisieren.
    LaunchedEffect(draft.published) {
        if (draft.published != null && nav.current == Screen.Compose) {
            nav.resetTo(Screen.Home, Screen.Success)
            homeVm.refresh()
        }
    }

    // Nach erfolgreichem Speichern: zurück zur Beitragsansicht.
    LaunchedEffect(edit.savedId) {
        val id = edit.savedId ?: return@LaunchedEffect
        editVm.consumeSaved()
        postVm.load(id, force = true)
        homeVm.refresh()
        nav.finishEdit(id)
    }

    // Beitragsansicht/Editor laden, sobald sie angezeigt werden.
    LaunchedEffect(screen) {
        when (screen) {
            is Screen.Post -> postVm.load(screen.id)
            is Screen.Edit -> editVm.start(screen.id)
            Screen.Calendar -> calendarVm.open()
            else -> Unit
        }
    }

    val finishSuccess = {
        draftVm.reset()
        nav.resetTo(Screen.Home)
    }

    BackHandler(enabled = nav.stack.size > 1) {
        if (nav.current == Screen.Success) finishSuccess() else nav.pop()
    }

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                settings = settings,
                home = home,
                draft = draft,
                onCompose = { nav.push(Screen.Compose) },
                onDiscardDraft = draftVm::discard,
                onOpenPost = { nav.push(Screen.Post(it)) },
                onRefresh = homeVm::refresh,
                onLoadMore = homeVm::loadMore,
                onSettings = { nav.push(Screen.Settings) },
                onDoc = { nav.push(Screen.Doc(it)) },
                onSearch = { nav.push(Screen.Search) },
                onCalendar = { nav.push(Screen.Calendar) },
            )
            Screen.Search -> SearchScreen(
                state = search,
                onBack = { nav.pop() },
                onQuery = searchVm::setQuery,
                onSubmit = searchVm::submit,
                onLoadMore = searchVm::loadMore,
                onRetry = searchVm::retry,
                onOpenPost = { nav.push(Screen.Post(it)) },
            )
            Screen.Calendar -> CalendarScreen(
                state = calendar,
                onBack = { nav.pop() },
                onPrevious = calendarVm::previousMonth,
                onNext = calendarVm::nextMonth,
                onToday = calendarVm::thisMonth,
                onSelect = calendarVm::select,
                onRetry = calendarVm::retry,
                onOpenPost = { nav.push(Screen.Post(it)) },
            )
            Screen.Compose -> ComposeScreen(
                state = draft,
                onBack = { nav.pop() },
                onTitle = draftVm::setTitle,
                onBefore = draftVm::setBefore,
                onAfter = draftVm::setAfter,
                onAddMedia = draftVm::addMedia,
                onRemove = draftVm::remove,
                onRetry = draftVm::retry,
                onMove = draftVm::move,
                onPublish = draftVm::publish,
                onDismissError = draftVm::dismissError,
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                repository = repository,
                onBack = { nav.pop() },
                onSaved = { firstSetup ->
                    draftVm.retryFailed()
                    if (firstSetup) nav.pop()
                },
                onLegal = { nav.push(Screen.Doc(it)) },
            )
            Screen.Success -> SuccessScreen(
                post = draft.published,
                onView = { id ->
                    draftVm.reset()
                    nav.resetTo(Screen.Home, Screen.Post(id))
                },
                onNewPost = {
                    draftVm.reset()
                    nav.resetTo(Screen.Home, Screen.Compose)
                },
                onDone = finishSuccess,
            )
            is Screen.Post -> PostScreen(
                state = if (post.id == s.id) post else PostViewState(id = s.id, loading = true),
                baseUrl = settings.credentials?.siteUrl ?: "",
                onHome = { nav.resetTo(Screen.Home) },
                onOlder = { nav.replaceTop(Screen.Post(it)) },
                onNewer = { nav.replaceTop(Screen.Post(it)) },
                onEdit = { nav.push(Screen.Edit(s.id)) },
                onRetry = { postVm.load(s.id, force = true) },
            )
            is Screen.Edit -> EditPostScreen(
                state = if (edit.postId == s.id) edit else EditState(postId = s.id, loading = true),
                onLeave = {
                    editVm.discard()
                    nav.pop()
                },
                onTitle = editVm::setTitle,
                onDate = editVm::setDate,
                onText = editVm::setText,
                onAddText = editVm::addText,
                onAddMedia = editVm::addMedia,
                onMove = editVm::move,
                onRemove = editVm::remove,
                onRetry = editVm::retry,
                onFeatured = editVm::setFeatured,
                onSave = editVm::save,
                onReload = { editVm.load(s.id) },
                onDismissError = editVm::dismissError,
            )
            is Screen.Doc -> LegalScreen(doc = s.doc, onBack = { nav.pop() })
        }
    }
}
