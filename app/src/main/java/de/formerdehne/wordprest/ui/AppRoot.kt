package de.formerdehne.wordprest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.formerdehne.wordprest.data.SettingsRepository
import de.formerdehne.wordprest.ui.screens.ComposeScreen
import de.formerdehne.wordprest.ui.screens.HomeScreen
import de.formerdehne.wordprest.ui.screens.LegalScreen
import de.formerdehne.wordprest.ui.screens.SettingsScreen
import de.formerdehne.wordprest.ui.screens.SuccessScreen

@Composable
fun AppRoot(nav: NavViewModel, draftVm: ComposeViewModel, repository: SettingsRepository) {
    val settings by repository.state.collectAsStateWithLifecycle()
    val draft by draftVm.state.collectAsStateWithLifecycle()
    val screen = nav.current

    // Nach erfolgreichem Veröffentlichen zur Erfolgsseite wechseln.
    LaunchedEffect(draft.published) {
        if (draft.published != null && nav.current == Screen.Compose) {
            nav.resetTo(Screen.Home, Screen.Success)
        }
    }

    val finish = {
        draftVm.reset()
        nav.resetTo(Screen.Home)
    }

    BackHandler(enabled = nav.stack.size > 1) {
        if (nav.current == Screen.Success) finish() else nav.pop()
    }

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                settings = settings,
                draft = draft,
                onCompose = { nav.push(Screen.Compose) },
                onDiscardDraft = draftVm::discard,
                onSettings = { nav.push(Screen.Settings) },
                onLegal = { nav.push(Screen.Legal(it)) },
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
                onLegal = { nav.push(Screen.Legal(it)) },
            )
            Screen.Success -> SuccessScreen(
                post = draft.published,
                onNewPost = {
                    draftVm.reset()
                    nav.resetTo(Screen.Home, Screen.Compose)
                },
                onDone = finish,
            )
            is Screen.Legal -> LegalScreen(doc = s.doc, onBack = { nav.pop() })
        }
    }
}
