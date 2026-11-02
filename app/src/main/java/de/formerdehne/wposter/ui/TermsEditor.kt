package de.formerdehne.wposter.ui

import de.formerdehne.wposter.R
import de.formerdehne.wposter.data.Term
import de.formerdehne.wposter.data.TermsCache
import de.formerdehne.wposter.i18n.str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TermsUi(
    val loading: Boolean = false,
    val error: String? = null,
    val availableCategories: List<Term> = emptyList(),
    val popularTags: List<Term> = emptyList(),
    val selectedCategories: List<Term> = emptyList(),
    val selectedTags: List<Term> = emptyList(),
    val tagQuery: String = "",
    val tagResults: List<Term> = emptyList(),
) {
    /** Vorschläge passend zur Eingabe (ohne bereits gewählte). */
    val tagSuggestions: List<Term>
        get() {
            val q = tagQuery.trim()
            val chosen = selectedTags.map { it.name.lowercase() }.toSet()
            val pool = if (q.isEmpty()) popularTags.take(12)
            else (popularTags.filter { it.name.contains(q, ignoreCase = true) } + tagResults)
            return pool.distinctBy { it.name.lowercase() }.filter { it.name.lowercase() !in chosen }.take(12)
        }
}

/** Auswahl von Kategorien und Schlagwörtern – gemeinsam für "Neuer Beitrag" und "Bearbeiten". */
class TermsEditor(
    private val scope: CoroutineScope,
    private val cache: TermsCache,
    private val onChange: () -> Unit = {},
) {
    private val _state = MutableStateFlow(TermsUi())
    val state: StateFlow<TermsUi> = _state.asStateFlow()
    private var accountId: String? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    /** Listen für den Blog laden; optional bereits gesetzte Begriffe (IDs) auswählen. */
    fun load(accountId: String, selectedCategoryIds: List<Long>? = null, selectedTagIds: List<Long>? = null) {
        val switched = this.accountId != accountId
        this.accountId = accountId
        if (switched) _state.value = TermsUi()
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = scope.launch {
            try {
                val terms = cache.get(accountId)
                var cats = _state.value.selectedCategories
                var tags = _state.value.selectedTags
                if (selectedCategoryIds != null) {
                    val known = terms.categories.filter { it.id in selectedCategoryIds }
                    val missing = selectedCategoryIds.filter { id -> known.none { it.id == id } }
                    cats = known + cache.byIds(accountId, "categories", missing)
                }
                if (selectedTagIds != null) {
                    val known = terms.tags.filter { it.id in selectedTagIds }
                    val missing = selectedTagIds.filter { id -> known.none { it.id == id } }
                    tags = known + cache.byIds(accountId, "tags", missing)
                }
                _state.update {
                    it.copy(
                        loading = false,
                        availableCategories = terms.categories,
                        popularTags = terms.tags,
                        selectedCategories = cats,
                        selectedTags = tags,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: str(R.string.common_unknown_error)) }
            }
        }
    }

    fun reset() {
        loadJob?.cancel()
        searchJob?.cancel()
        accountId = null
        _state.value = TermsUi()
    }

    fun toggleCategory(term: Term) {
        _state.update { s ->
            val selected = s.selectedCategories.any { same(it, term) }
            s.copy(selectedCategories = if (selected) s.selectedCategories.filterNot { same(it, term) } else s.selectedCategories + term)
        }
        onChange()
    }

    /** Neue Kategorie vormerken (wird beim Speichern angelegt) bzw. vorhandene auswählen. */
    fun addCategory(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        val existing = _state.value.availableCategories.find { it.name.equals(n, ignoreCase = true) }
        _state.update { s ->
            val term = existing ?: Term(null, n)
            s.copy(
                availableCategories = if (existing == null) s.availableCategories + term else s.availableCategories,
                selectedCategories = if (s.selectedCategories.any { same(it, term) }) s.selectedCategories else s.selectedCategories + term,
            )
        }
        onChange()
    }

    fun addTag(term: Term) {
        _state.update { s ->
            if (s.selectedTags.any { it.name.equals(term.name, ignoreCase = true) }) s.copy(tagQuery = "")
            else s.copy(selectedTags = s.selectedTags + term, tagQuery = "", tagResults = emptyList())
        }
        onChange()
    }

    /** Eingegebenen Text als Schlagwort übernehmen (vorhandenes wiederverwenden, sonst neu). */
    fun addTagFromQuery() {
        val n = _state.value.tagQuery.trim().trimEnd(',')
        if (n.isEmpty()) return
        val s = _state.value
        val existing = (s.popularTags + s.tagResults).find { it.name.equals(n, ignoreCase = true) }
        addTag(existing ?: Term(null, n))
    }

    fun removeTag(term: Term) {
        _state.update { s -> s.copy(selectedTags = s.selectedTags.filterNot { same(it, term) }) }
        onChange()
    }

    fun setTagQuery(query: String) {
        // Komma oder Enter schließt ein Schlagwort ab.
        if (query.endsWith(",")) {
            _state.update { it.copy(tagQuery = query) }
            addTagFromQuery()
            return
        }
        _state.update { it.copy(tagQuery = query) }
        searchJob?.cancel()
        val id = accountId ?: return
        if (query.trim().length < 2) return
        searchJob = scope.launch {
            delay(350)
            val results = runCatching { cache.searchTags(id, query.trim()) }.getOrDefault(emptyList())
            _state.update { if (it.tagQuery == query) it.copy(tagResults = results) else it }
        }
    }

    /** IDs für das Speichern; neue Begriffe werden dabei angelegt. */
    suspend fun resolveIds(accountId: String): Pair<List<Long>, List<Long>> {
        val s = _state.value
        val cats = cache.resolve(accountId, "categories", s.selectedCategories)
        val tags = cache.resolve(accountId, "tags", s.selectedTags)
        return cats to tags
    }

    private fun same(a: Term, b: Term) =
        if (a.id != null && b.id != null) a.id == b.id else a.name.equals(b.name, ignoreCase = true)
}
