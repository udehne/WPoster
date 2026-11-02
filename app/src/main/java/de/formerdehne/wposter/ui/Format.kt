package de.formerdehne.wposter.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Format {
    // Locale bei jedem Aufruf neu lesen, damit ein Sprachwechsel sofort greift.
    // Zeit nur als SHORT: LONG/FULL bräuchte eine Zeitzone, LocalDateTime hat keine.
    private fun day() = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.getDefault())

    private fun dayTime() = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())

    fun date(d: LocalDateTime?): String = d?.format(day()).orEmpty()
    fun dateTime(d: LocalDateTime?): String = d?.format(dayTime()).orEmpty()
}
