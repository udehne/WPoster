package de.formerdehne.wordprest.data

import org.json.JSONObject

/** Ein Block auf oberster Ebene eines Beitrags. [raw] ist der unveränderte Originaltext. */
sealed interface ParsedBlock {
    val raw: String

    data class Paragraph(override val raw: String, val text: String, val hasFormatting: Boolean) : ParsedBlock
    data class Heading(override val raw: String, val text: String, val level: Int, val hasFormatting: Boolean) : ParsedBlock
    data class Image(override val raw: String, val id: Long, val src: String?) : ParsedBlock
    data class Video(override val raw: String, val id: Long, val src: String?) : ParsedBlock
    data class Other(override val raw: String, val label: String, val summary: String) : ParsedBlock
}

/**
 * Zerlegt den Roh-Inhalt eines Beitrags (content.raw) in Blöcke der obersten
 * Ebene. Verschachtelte Blöcke (Spalten, Gruppen, …) bleiben als Ganzes erhalten.
 */
object BlockParser {

    private val token = Regex(
        "<!--\\s+(/)?wp:([a-z][a-z0-9_-]*(?:/[a-z][a-z0-9_-]*)?)(\\s+\\{.*?\\})?\\s*(/)?-->",
        RegexOption.DOT_MATCHES_ALL
    )
    private val pTag = Regex("<p\\b[^>]*>(.*)</p>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val hTag = Regex("<h([1-6])\\b[^>]*>(.*)</h\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val imgSrc = Regex("<img\\b[^>]*?\\bsrc=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val videoSrc = Regex("<video\\b[^>]*?\\bsrc=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    private val wpImageClass = Regex("wp-image-(\\d+)")

    fun parse(content: String): List<ParsedBlock> {
        val out = mutableListOf<ParsedBlock>()
        var depth = 0
        var start = 0
        var innerStart = 0
        var name = ""
        var attrs = ""
        var cursor = 0

        for (m in token.findAll(content)) {
            val closing = m.groupValues[1] == "/"
            val selfClosing = m.groupValues[4] == "/"
            val n = m.groupValues[2]
            if (depth == 0) {
                if (closing) continue // verirrtes Endkommentar ignorieren
                freeform(content.substring(cursor, m.range.first))?.let { out += it }
                if (selfClosing) {
                    out += classify(n, m.groupValues[3], m.value, "")
                    cursor = m.range.last + 1
                } else {
                    depth = 1
                    start = m.range.first
                    innerStart = m.range.last + 1
                    name = n
                    attrs = m.groupValues[3]
                }
            } else if (closing) {
                depth--
                if (depth == 0) {
                    out += classify(
                        name, attrs,
                        content.substring(start, m.range.last + 1),
                        content.substring(innerStart, m.range.first)
                    )
                    cursor = m.range.last + 1
                }
            } else if (!selfClosing) {
                depth++
            }
        }
        val rest = if (depth > 0) content.substring(start) else content.substring(cursor)
        freeform(rest)?.let { out += it }
        return out
    }

    private fun freeform(text: String): ParsedBlock? {
        if (text.isBlank()) return null
        return ParsedBlock.Other(text.trim(), "Klassischer Inhalt", excerpt(text))
    }

    private fun classify(name: String, attrsText: String, raw: String, inner: String): ParsedBlock {
        val attrs = attrsText.trim().takeIf { it.isNotEmpty() }?.let {
            try { JSONObject(it) } catch (_: Exception) { null }
        }
        return when (name) {
            "paragraph" -> {
                val html = pTag.find(inner)?.groupValues?.get(1) ?: inner
                ParsedBlock.Paragraph(raw, HtmlText.toPlain(html).trim(), HtmlText.hasFormatting(html))
            }
            "heading" -> {
                val match = hTag.find(inner)
                val html = match?.groupValues?.get(2) ?: inner
                val level = attrs?.optInt("level", 0)?.takeIf { it in 1..6 }
                    ?: match?.groupValues?.get(1)?.toIntOrNull() ?: 2
                ParsedBlock.Heading(raw, HtmlText.toPlain(html).trim(), level, HtmlText.hasFormatting(html))
            }
            "image" -> {
                val id = attrs?.optLong("id", 0L)?.takeIf { it > 0 }
                    ?: wpImageClass.find(inner)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                ParsedBlock.Image(raw, id, imgSrc.find(inner)?.groupValues?.get(1)?.let { HtmlText.decode(it) })
            }
            "video" -> ParsedBlock.Video(
                raw,
                attrs?.optLong("id", 0L) ?: 0L,
                videoSrc.find(inner)?.groupValues?.get(1)?.let { HtmlText.decode(it) }
            )
            "gallery" -> {
                val count = imgSrc.findAll(inner).count()
                ParsedBlock.Other(raw, "Galerie", if (count == 1) "1 Bild" else "$count Bilder")
            }
            else -> ParsedBlock.Other(raw, label(name), excerpt(inner))
        }
    }

    private fun label(name: String) = when (name) {
        "list" -> "Liste"
        "quote" -> "Zitat"
        "embed" -> "Eingebetteter Inhalt"
        "columns" -> "Spalten"
        "group" -> "Gruppe"
        "cover" -> "Titelbild"
        "separator" -> "Trennlinie"
        "spacer" -> "Abstand"
        "audio" -> "Audio"
        "file" -> "Datei"
        "media-text" -> "Medien und Text"
        "html" -> "HTML"
        else -> "Block „$name“"
    }

    private fun excerpt(html: String): String {
        val plain = HtmlText.toPlain(html).replace(Regex("\\s+"), " ").trim()
        return if (plain.length > 120) plain.take(117) + "…" else plain
    }
}
