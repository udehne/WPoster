package de.formerdehne.wposter.data

/** Kategorien und häufige Schlagwörter je Blog (für die Auswahl in den Editoren). */
class TermsCache(private val settings: SettingsRepository) {

    data class Terms(val categories: List<Term>, val tags: List<Term>)

    private val cache = HashMap<String, Terms>()

    suspend fun get(accountId: String, refresh: Boolean = false): Terms {
        if (!refresh) cache[accountId]?.let { return it }
        val client = client(accountId) ?: return Terms(emptyList(), emptyList())
        val terms = Terms(client.categories(), client.popularTags())
        cache[accountId] = terms
        return terms
    }

    suspend fun searchTags(accountId: String, query: String): List<Term> =
        client(accountId)?.searchTags(query).orEmpty()

    suspend fun byIds(accountId: String, taxonomy: String, ids: List<Long>): List<Term> =
        client(accountId)?.termsByIds(taxonomy, ids).orEmpty()

    /** Neue (noch nicht angelegte) Begriffe anlegen und alle IDs liefern. */
    suspend fun resolve(accountId: String, taxonomy: String, terms: List<Term>): List<Long> {
        val client = client(accountId) ?: return terms.mapNotNull { it.id }
        val ids = terms.map { it.id ?: client.createTerm(taxonomy, it.name).id }.filterNotNull()
        if (terms.any { it.id == null }) cache.remove(accountId)
        return ids
    }

    fun clear(accountId: String) = cache.remove(accountId)

    private fun client(accountId: String) = settings.state.value.account(accountId)?.let { WpClient(it.credentials) }
}
