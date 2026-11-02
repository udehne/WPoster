package de.formerdehne.wposter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.formerdehne.wposter.ui.screens.openInBrowser
import de.formerdehne.wposter.data.SettingsRepository
import de.formerdehne.wposter.ui.screens.AccountScreen
import de.formerdehne.wposter.ui.screens.CalendarScreen
import de.formerdehne.wposter.ui.screens.CongratsScreen
import de.formerdehne.wposter.ui.screens.OnboardingScreen
import de.formerdehne.wposter.ui.screens.SharePickScreen
import de.formerdehne.wposter.ui.screens.ComposeScreen
import de.formerdehne.wposter.ui.screens.SearchScreen
import de.formerdehne.wposter.ui.screens.EditPostScreen
import de.formerdehne.wposter.ui.screens.HomeScreen
import de.formerdehne.wposter.ui.screens.LegalScreen
import de.formerdehne.wposter.ui.screens.PostScreen
import de.formerdehne.wposter.ui.screens.SettingsScreen
import de.formerdehne.wposter.ui.screens.SuccessScreen

@Composable
fun AppRoot(
    nav: NavViewModel,
    draftVm: ComposeViewModel,
    homeVm: HomeViewModel,
    postVm: PostViewModel,
    editVm: EditViewModel,
    searchVm: SearchViewModel,
    calendarVm: CalendarViewModel,
    onboardingVm: OnboardingViewModel,
    repository: SettingsRepository,
) {
    val settings by repository.state.collectAsStateWithLifecycle()
    val draft by draftVm.state.collectAsStateWithLifecycle()
    val home by homeVm.state.collectAsStateWithLifecycle()
    val post by postVm.state.collectAsStateWithLifecycle()
    val edit by editVm.state.collectAsStateWithLifecycle()
    val search by searchVm.state.collectAsStateWithLifecycle()
    val calendar by calendarVm.state.collectAsStateWithLifecycle()
    val onboarding by onboardingVm.state.collectAsStateWithLifecycle()
    val draftTerms by draftVm.terms.state.collectAsStateWithLifecycle()
    val editTerms by editVm.terms.state.collectAsStateWithLifecycle()
    val screen = nav.current
    val context = LocalContext.current

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

    // Blog erfolgreich eingerichtet.
    LaunchedEffect(onboarding.doneAccountId) {
        if (onboarding.doneAccountId == null) return@LaunchedEffect
        val siteName = onboarding.doneSiteName
        val wasFirst = onboarding.wasFirst
        onboardingVm.consumeDone()
        draftVm.retryFailed()
        when {
            // Kam vom Teilen ohne eingerichteten Blog → zurück zum Entwurf.
            nav.stack.contains(Screen.Compose) -> {
                if (nav.current == Screen.Onboarding) nav.pop()
                draftVm.loadTerms()
            }
            wasFirst -> nav.resetTo(Screen.Home, Screen.Congrats(siteName))
            else -> nav.resetTo(Screen.Home)
        }
    }

    fun startOnboarding() {
        onboardingVm.start()
        nav.push(Screen.Onboarding)
    }

    // Beitragsansicht/Editor laden, sobald sie angezeigt werden.
    LaunchedEffect(screen) {
        when (screen) {
            is Screen.Post -> postVm.load(screen.id)
            is Screen.Edit -> editVm.start(screen.id)
            Screen.Calendar -> calendarVm.open()
            Screen.Compose -> draftVm.loadTerms()
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
                onCompose = {
                    // Offener Entwurf (ggf. für einen anderen Blog) wird fortgesetzt, sonst neuer Entwurf im aktiven Blog.
                    if (draft.hasContent) {
                        draft.siteId?.let { if (it != settings.active?.id) repository.setActive(it) }
                    } else {
                        settings.active?.id?.let(draftVm::startFor)
                    }
                    nav.push(Screen.Compose)
                },
                onDiscardDraft = draftVm::discard,
                onOpenPost = { nav.push(Screen.Post(it)) },
                onRefresh = homeVm::refresh,
                onLoadMore = homeVm::loadMore,
                onSettings = { nav.push(Screen.Settings) },
                onDoc = { nav.push(Screen.Doc(it)) },
                onSearch = { nav.push(Screen.Search) },
                onCalendar = { nav.push(Screen.Calendar) },
                onOpenUrl = { openInBrowser(context, it) },
                onDismissUpdate = homeVm::dismissUpdate,
                onStartOnboarding = ::startOnboarding,
                onSwitchBlog = repository::setActive,
                onAddBlog = ::startOnboarding,
                onContinueDraft = {
                    draft.siteId?.let { if (it != settings.active?.id) repository.setActive(it) }
                    nav.push(Screen.Compose)
                },
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
                siteLabel = settings.takeIf { it.accounts.size > 1 }
                    ?.let { it.account(draft.siteId) ?: it.active }
                    ?.let { it.siteName.ifBlank { it.host } },
                terms = draftTerms,
                termsEditor = draftVm.terms,
                onRetryTerms = draftVm::loadTerms,
                onCaption = draftVm::setCaption,
                onPublishNow = draftVm::setPublishNow,
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                repository = repository,
                onBack = { nav.pop() },
                onOpenAccount = { nav.push(Screen.Account(it)) },
                onAddBlog = ::startOnboarding,
                onLegal = { nav.push(Screen.Doc(it)) },
            )
            is Screen.Account -> {
                val account = settings.account(s.id)
                if (account == null) {
                    LaunchedEffect(s.id) { nav.pop() }
                } else {
                    AccountScreen(
                        account = account,
                        isStart = settings.startId == account.id,
                        isOnlyAccount = settings.accounts.size == 1,
                        repository = repository,
                        onBack = { nav.pop() },
                        onRemoved = {
                            if (settings.accounts.size <= 1) nav.resetTo(Screen.Home) else nav.pop()
                        },
                    )
                }
            }
            Screen.Onboarding -> OnboardingScreen(
                state = onboarding,
                vm = onboardingVm,
                onCancel = { nav.pop() },
            )
            is Screen.Congrats -> CongratsScreen(
                siteName = s.siteName,
                onContinue = { nav.resetTo(Screen.Home) },
            )
            Screen.SharePick -> SharePickScreen(
                accounts = settings.accounts,
                activeId = settings.active?.id,
                draftSiteId = draft.siteId,
                draftHasContent = draft.hasContent,
                onPick = { id ->
                    repository.setActive(id)
                    draftVm.startFor(id)
                    draftVm.addMedia(draftVm.pendingShare, stage = true)
                    draftVm.pendingShare = emptyList()
                    nav.resetTo(Screen.Home, Screen.Compose)
                },
                onCancel = {
                    draftVm.pendingShare = emptyList()
                    nav.resetTo(Screen.Home)
                },
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
                terms = editTerms,
                termsEditor = editVm.terms,
                onRetryTerms = { edit.siteId?.let { editVm.terms.load(it) } },
                onCaption = editVm::setCaption,
                onDraft = editVm::setDraft,
            )
            is Screen.Doc -> LegalScreen(doc = s.doc, onBack = { nav.pop() })
        }
    }
}
