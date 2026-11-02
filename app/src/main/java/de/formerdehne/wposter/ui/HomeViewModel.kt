package de.formerdehne.wposter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wposter.R
import de.formerdehne.wposter.i18n.str
import de.formerdehne.wposter.WPosterApp
import de.formerdehne.wposter.data.PostSummary
import de.formerdehne.wposter.data.UpdateChecker
import de.formerdehne.wposter.data.WpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val posts: List<PostSummary> = emptyList(),
    val initialLoading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WPosterApp).settings
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var page = 0
    private var job: Job? = null

    init {
        checkForUpdate()
        // Neu laden, sobald (andere) Zugangsdaten gespeichert werden.
        viewModelScope.launch {
            settings.state
                .map { it.credentials?.let { c -> c.siteUrl + "|" + c.username } }
                .distinctUntilChanged()
                .collect { key ->
                    job?.cancel()
                    _state.value = HomeState()
                    if (key != null) load(reset = true, initial = true)
                }
        }
    }

    fun refresh() = load(reset = true, initial = _state.value.posts.isEmpty())

    fun loadMore() {
        val s = _state.value
        if (s.endReached || s.loadingMore || s.refreshing || s.initialLoading || s.error != null) return
        load(reset = false, initial = false)
    }

    private fun load(reset: Boolean, initial: Boolean) {
        val credentials = settings.state.value.credentials ?: return
        job?.cancel()
        _state.update {
            it.copy(
                initialLoading = initial,
                refreshing = reset && !initial,
                loadingMore = !reset,
                error = null,
            )
        }
        job = viewModelScope.launch {
            val client = WpClient(credentials)
            try {
                val nextPage = if (reset) 1 else page + 1
                val posts = client.listPosts(nextPage, PAGE_SIZE)
                page = nextPage
                _state.update {
                    it.copy(
                        posts = if (reset) posts else (it.posts + posts).distinctBy { p -> p.id },
                        endReached = posts.size < PAGE_SIZE,
                        initialLoading = false, refreshing = false, loadingMore = false,
                    )
                }
                if (reset) {
                    // Titel/Untertitel des Blogs nebenbei aktuell halten.
                    runCatching { client.siteMeta() }.getOrNull()?.let {
                        settings.updateSiteMeta(it.name, it.description)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: str(R.string.common_unknown_error),
                        initialLoading = false, refreshing = false, loadingMore = false,
                    )
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Höchstens alle 6 Stunden bei GitHub nach einer neuen Version fragen. */
    private fun checkForUpdate() {
        val s = settings.state.value
        if (!s.checkUpdates || !UpdateChecker.enabledForThisBuild) return
        if (System.currentTimeMillis() - s.lastUpdateCheck < 6 * 60 * 60 * 1000L) return
        viewModelScope.launch {
            val update = runCatching { UpdateChecker.latest() }.getOrNull() ?: return@launch
            settings.saveUpdateCheck(update.version, update.apkUrl ?: update.pageUrl)
        }
    }

    fun dismissUpdate(version: String) = settings.dismissUpdate(version)

    companion object {
        const val PAGE_SIZE = 20
    }
}
