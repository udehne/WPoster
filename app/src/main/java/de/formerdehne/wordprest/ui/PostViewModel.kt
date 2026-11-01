package de.formerdehne.wordprest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.formerdehne.wordprest.WordPrestApp
import de.formerdehne.wordprest.data.PostDetail
import de.formerdehne.wordprest.data.WpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostViewState(
    val id: Long = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val post: PostDetail? = null,
    val olderId: Long? = null,
    val newerId: Long? = null,
)

class PostViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = (app as WordPrestApp).settings
    private val _state = MutableStateFlow(PostViewState())
    val state: StateFlow<PostViewState> = _state.asStateFlow()
    private var job: Job? = null

    fun load(id: Long, force: Boolean = false) {
        val s = _state.value
        if (!force && s.id == id && (s.post != null || s.loading)) return
        val credentials = settings.state.value.credentials ?: return
        job?.cancel()
        _state.value = PostViewState(id = id, loading = true)
        job = viewModelScope.launch {
            val client = WpClient(credentials)
            try {
                val post = client.getPost(id)
                _state.update { it.copy(post = post, loading = false) }
                val (older, newer) = try {
                    client.neighbors(post.dateRaw)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null to null
                }
                _state.update { it.copy(olderId = older, newerId = newer) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unbekannter Fehler") }
            }
        }
    }
}
