package de.formerdehne.wposter.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

enum class InfoDoc { HILFE, IMPRESSUM, DATENSCHUTZ }

sealed interface Screen {
    data object Home : Screen
    data object Compose : Screen
    data object Settings : Screen
    data object Success : Screen
    data object Search : Screen
    data object Calendar : Screen
    data class Post(val id: Long) : Screen
    data class Edit(val id: Long) : Screen
    data class Doc(val doc: InfoDoc) : Screen
    /** Blog einrichten (3 Schritte). */
    data object Onboarding : Screen
    data class Congrats(val siteName: String) : Screen
    data class Account(val id: String) : Screen
    /** Blog-Auswahl beim Teilen, wenn mehrere Blogs eingerichtet sind. */
    data object SharePick : Screen
}

/** Kleine, eigene Navigation mit Zurück-Stapel. */
class NavViewModel(app: Application) : AndroidViewModel(app) {

    // Ohne Blog zeigt die Startseite die Begrüßung (siehe HomeScreen/WelcomeScreen).
    val stack = mutableStateListOf<Screen>(Screen.Home)

    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Aktuellen Bildschirm ersetzen (z. B. zum nächsten Beitrag blättern). */
    fun replaceTop(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun resetTo(vararg screens: Screen) {
        stack.clear()
        stack.addAll(screens.toList().ifEmpty { listOf(Screen.Home) })
    }

    /** Nach dem Speichern: Editor schließen und die Beitragsansicht zeigen. */
    fun finishEdit(id: Long) {
        if (current is Screen.Edit) pop()
        if (current != Screen.Post(id)) push(Screen.Post(id))
    }

    /** Für geteilte Medien: direkt zum Beitrags-Editor (ohne doppelte Einträge). */
    fun openCompose(needsSetup: Boolean) {
        resetTo(Screen.Home, Screen.Compose)
        if (needsSetup) stack.add(Screen.Onboarding)
    }
}
