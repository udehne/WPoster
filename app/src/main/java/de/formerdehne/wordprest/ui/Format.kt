package de.formerdehne.wordprest.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val day = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)
    private val dayTime = DateTimeFormatter.ofPattern("d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)

    fun date(d: LocalDateTime?): String = d?.format(day).orEmpty()
    fun dateTime(d: LocalDateTime?): String = d?.format(dayTime).orEmpty()
}
