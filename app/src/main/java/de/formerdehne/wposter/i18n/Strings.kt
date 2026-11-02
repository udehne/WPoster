package de.formerdehne.wposter.i18n

import androidx.annotation.StringRes
import de.formerdehne.wposter.WPosterApp

/** Übersetzten Text außerhalb von Composables holen (ViewModels, Datenschicht). */
fun str(@StringRes id: Int, vararg args: Any): String = WPosterApp.instance.getString(id, *args)
