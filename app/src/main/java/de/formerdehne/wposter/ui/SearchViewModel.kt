package de.formerdehne.wposter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.PostSummary
import de.formerdehne.wposter.data.WpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    /** Suchbegriff, zu dem [results] gehören. */
    val searchedFor: String? = null,
    val results: List<PostSummary> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WPosterApp).settings
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var job: Job? = null
    private var page = 0

    /** Während des Tippens: kurz warten, dann suchen. */
    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        job?.cancel()
        if (q.trim().length < MIN_LENGTH) {
            _state.update { it.copy(searchedFor = null, results = emptyList(), loading = false, error = null) }
            return
        }
        job = viewModelScope.launch {
            delay(400)
            run(reset = true)
        }
    }

    /** Suche sofort ausführen (Enter auf der Tastatur). */
    fun submit() {
        if (_state.value.query.trim().length < MIN_LENGTH) return
        job?.cancel()
        job = viewModelScope.launch { run(reset = true) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.endReached || s.loading || s.loadingMore || s.error != null || s.searchedFor == null) return
        job = viewModelScope.launch { run(reset = false) }
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        if (_state.value.results.isEmpty()) submit() else loadMore()
    }

    private suspend fun run(reset: Boolean) {
        val credentials = settings.state.value.credentials ?: return
        val term = if (reset) _state.value.query.trim() else _state.value.searchedFor ?: return
        _state.update { it.copy(loading = reset, loadingMore = !reset, error = null) }
        try {
            val nextPage = if (reset) 1 else page + 1
            val posts = WpClient(credentials).listPosts(nextPage, PAGE_SIZE, search = term)
            page = nextPage
            _state.update {
                it.copy(
                    searchedFor = term,
                    results = if (reset) posts else (it.results + posts).distinctBy { p -> p.id },
                    endReached = posts.size < PAGE_SIZE,
                    loading = false, loadingMore = false,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, loadingMore = false, error = e.message ?: "Unbekannter Fehler") }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MIN_LENGTH = 2
    }
}
