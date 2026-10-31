package de.formerdehne.wordprest.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import de.formerdehne.wordprest.WordPrestApp

enum class LegalDoc { IMPRESSUM, DATENSCHUTZ }

sealed interface Screen {
    data object Home : Screen
    data object Compose : Screen
    data object Settings : Screen
    data object Success : Screen
    data class Legal(val doc: LegalDoc) : Screen
}

/** Kleine, eigene Navigation mit Zurück-Stapel. */
class NavViewModel(app: Application) : AndroidViewModel(app) {

    val stack = mutableStateListOf<Screen>(Screen.Home).apply {
        if ((app as WordPrestApp).settings.state.value.credentials == null) add(Screen.Settings)
    }

    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun resetTo(vararg screens: Screen) {
        stack.clear()
        stack.addAll(screens.toList().ifEmpty { listOf(Screen.Home) })
    }

    /** Für geteilte Medien: direkt zum Beitrags-Editor (ohne doppelte Einträge). */
    fun openCompose(needsSetup: Boolean) {
        resetTo(Screen.Home, Screen.Compose)
        if (needsSetup) stack.add(Screen.Settings)
    }
}
