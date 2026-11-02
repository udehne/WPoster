package de.formerdehne.wposter.data

import org.json.JSONObject

/** Ein Block auf oberster Ebene eines Beitrags. [raw] ist der unveränderte Originaltext. */
sealed interface ParsedBlock {
    val raw: String

    data class Paragraph(override val raw: String, val text: String, val hasFormatting: Boolean) : ParsedBlock
    data class Heading(override val raw: String, val text: String, val level: Int, val hasFormatting: Boolean) : ParsedBlock
    data class Image(
        override val raw: String,
        val id: Long,
        val src: String?,
        val caption: String = "",
        val credit: String = "",
    ) : ParsedBlock

    data class Video(
        override val raw: String,
        val id: Long,
        val src: String?,
        val caption: String = "",
        val credit: String = "",
    ) : ParsedBlock
    /**
     * Nicht bearbeitbarer Block. [blockName] ist der Blockname ohne "core/"-Präfix
     * (z. B. "gallery", "list") bzw. [FREEFORM] für klassischen Inhalt; [imageCount]
     * nur bei Galerien, [summary] ist ein Textauszug (leer bei Galerien).
     * Die Beschriftung für die Oberfläche entsteht erst in der UI (übersetzt).
     */
    data class Other(
        override val raw: String,
        val blockName: String,
        val imageCount: Int,
        val summary: String,
    ) : ParsedBlock {
        companion object {
            const val FREEFORM = "freeform"
        }
    }
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
    private val figcaption = Regex("<figcaption\\b[^>]*>(.*?)</figcaption>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

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

    /**
     * Bildunterschrift aus <figcaption> lesen. Eine eigene Zeile, die mit einem der
     * [BlockBuilder.CREDIT_PREFIXES] beginnt (z. B. "Foto: …"), gilt als Credit.
     */
    fun caption(inner: String): Pair<String, String> {
        val html = figcaption.find(inner)?.groupValues?.get(1) ?: return "" to ""
        val lines = HtmlText.toPlain(html).lines().map { it.trim() }.filter { it.isNotEmpty() }
        val creditLine = lines.lastOrNull()?.takeIf { line -> BlockBuilder.CREDIT_PREFIXES.any { line.startsWith(it) } }
        val credit = creditLine?.substringAfter(':')?.trim().orEmpty()
        val caption = (if (creditLine != null) lines.dropLast(1) else lines).joinToString("\n")
        return caption to credit
    }

    private fun freeform(text: String): ParsedBlock? {
        if (text.isBlank()) return null
        return ParsedBlock.Other(text.trim(), ParsedBlock.Other.FREEFORM, 0, excerpt(text))
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
                val (caption, credit) = caption(inner)
                ParsedBlock.Image(raw, id, imgSrc.find(inner)?.groupValues?.get(1)?.let { HtmlText.decode(it) }, caption, credit)
            }
            "video" -> {
                val (caption, credit) = caption(inner)
                ParsedBlock.Video(
                    raw,
                    attrs?.optLong("id", 0L) ?: 0L,
                    videoSrc.find(inner)?.groupValues?.get(1)?.let { HtmlText.decode(it) },
                    caption, credit,
                )
            }
            "gallery" -> {
                val count = imgSrc.findAll(inner).count()
                ParsedBlock.Other(raw, name, count, "")
            }
            else -> ParsedBlock.Other(raw, name, 0, excerpt(inner))
        }
    }

    private fun excerpt(html: String): String {
        val plain = HtmlText.toPlain(html).replace(Regex("\\s+"), " ").trim()
        return if (plain.length > 120) plain.take(117) + "…" else plain
    }
}
