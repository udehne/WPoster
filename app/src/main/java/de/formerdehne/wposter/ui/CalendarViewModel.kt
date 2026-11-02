package de.formerdehne.wposter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.R
import de.formerdehne.wposter.i18n.str
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.PostSummary
import de.formerdehne.wposter.data.WpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CalendarState(
    val month: YearMonth = YearMonth.now(),
    val posts: List<PostSummary> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val selectedDay: LocalDate? = null,
) {
    /** Anzahl Beiträge je Tag im angezeigten Monat. */
    val countByDay: Map<LocalDate, Int>
        get() = posts.mapNotNull { it.date?.toLocalDate() }.groupingBy { it }.eachCount()

    val visiblePosts: List<PostSummary>
        get() = selectedDay?.let { day -> posts.filter { it.date?.toLocalDate() == day } } ?: posts
}

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WPosterApp).settings
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()
    private val cache = HashMap<YearMonth, List<PostSummary>>()
    private var job: Job? = null

    /** Beim Öffnen/Zurückkehren: angezeigten Monat still aktualisieren, Auswahl behalten. */
    fun open() = show(_state.value.month, force = true)

    fun previousMonth() = show(_state.value.month.minusMonths(1))
    fun nextMonth() = show(_state.value.month.plusMonths(1))
    fun thisMonth() = show(YearMonth.now())

    fun select(day: LocalDate?) = _state.update {
        it.copy(selectedDay = if (it.selectedDay == day) null else day)
    }

    fun retry() = show(_state.value.month, force = true)

    private fun show(month: YearMonth, force: Boolean = false) {
        job?.cancel()
        val current = _state.value
        val keepDay = current.selectedDay?.takeIf { YearMonth.from(it) == month }
        val cached = cache[month]
        if (cached != null && !force) {
            _state.value = CalendarState(month = month, posts = cached, selectedDay = keepDay)
            return
        }
        val credentials = settings.state.value.credentials ?: return
        // Gleicher Monat mit vorhandenen Daten: im Hintergrund aktualisieren, ohne Ladeanzeige.
        val silent = current.month == month && current.posts.isNotEmpty()
        if (!silent) _state.value = CalendarState(month = month, posts = cached.orEmpty(), loading = cached == null, selectedDay = keepDay)
        job = viewModelScope.launch {
            try {
                val client = WpClient(credentials)
                // "after"/"before" sind exklusiv: letzter Moment des Vormonats bis Anfang Folgemonat.
                val after = month.atDay(1).atStartOfDay().minusSeconds(1).format(WP_DATE)
                val before = month.plusMonths(1).atDay(1).atStartOfDay().format(WP_DATE)
                val all = mutableListOf<PostSummary>()
                var page = 1
                while (page <= MAX_PAGES) {
                    val batch = client.listPosts(page, PAGE_SIZE, after = after, before = before)
                    all += batch
                    if (batch.size < PAGE_SIZE) break
                    page++
                }
                cache[month] = all
                _state.update { CalendarState(month = month, posts = all, selectedDay = it.selectedDay?.takeIf { d -> YearMonth.from(d) == month }) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (silent) return@launch // alte Daten bleiben sichtbar
                _state.value = CalendarState(month = month, error = e.message ?: str(R.string.common_unknown_error))
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 5
        private val WP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}
