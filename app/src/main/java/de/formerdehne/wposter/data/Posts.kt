package de.formerdehne.wposter.data

import org.json.JSONObject
import java.time.LocalDateTime

data class SiteMeta(val name: String, val description: String)

data class PostSummary(
    val id: Long,
    val title: String,
    val date: LocalDateTime?,
    val author: String?,
    val imageUrl: String?,
    val status: String,
)

data class PostDetail(
    val id: Long,
    val title: String,
    val contentHtml: String,
    val date: LocalDateTime?,
    /** Datum so, wie WordPress es liefert (für Abfragen nach Nachbarbeiträgen). */
    val dateRaw: String,
    val author: String?,
    val link: String,
    val status: String,
)

data class PostEditData(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDateTime?,
    val featuredMedia: Long,
    val status: String,
)

internal object PostJson {

    fun date(s: String?): LocalDateTime? = try {
        s?.takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) }
    } catch (_: Exception) {
        null
    }

    private fun rendered(j: JSONObject, key: String) =
        j.optJSONObject(key)?.optString("rendered").orEmpty()

    private fun authorName(j: JSONObject): String? =
        j.optJSONObject("_embedded")?.optJSONArray("author")?.optJSONObject(0)
            ?.optString("name")?.takeIf { it.isNotBlank() }?.let { HtmlText.decode(it) }

    fun summary(j: JSONObject): PostSummary {
        val media = j.optJSONObject("_embedded")?.optJSONArray("wp:featuredmedia")?.optJSONObject(0)
        val sizes = media?.optJSONObject("media_details")?.optJSONObject("sizes")
        val image = listOf("medium_large", "large", "full").firstNotNullOfOrNull { size ->
            sizes?.optJSONObject(size)?.optString("source_url")?.takeIf { it.isNotBlank() }
        } ?: media?.optString("source_url")?.takeIf { it.isNotBlank() }
        return PostSummary(
            id = j.getLong("id"),
            title = HtmlText.toPlain(rendered(j, "title")).trim(),
            date = date(j.optString("date")),
            author = authorName(j),
            imageUrl = image,
            status = j.optString("status", "publish"),
        )
    }

    fun detail(j: JSONObject) = PostDetail(
        id = j.getLong("id"),
        title = HtmlText.toPlain(rendered(j, "title")).trim(),
        contentHtml = rendered(j, "content"),
        date = date(j.optString("date")),
        dateRaw = j.optString("date"),
        author = authorName(j),
        link = j.optString("link"),
        status = j.optString("status", "publish"),
    )

    fun edit(j: JSONObject) = PostEditData(
        id = j.getLong("id"),
        title = j.optJSONObject("title")?.optString("raw").orEmpty(),
        content = j.optJSONObject("content")?.optString("raw").orEmpty(),
        date = date(j.optString("date")),
        featuredMedia = j.optLong("featured_media", 0L),
        status = j.optString("status", "publish"),
    )
}
