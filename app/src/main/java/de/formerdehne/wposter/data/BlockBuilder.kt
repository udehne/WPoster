package de.formerdehne.wposter.data

/** Erzeugt Inhalte im Format des Block-Editors (Gutenberg). */
object BlockBuilder {

    /** Präfixe, an denen eine Credit-Zeile in der Bildunterschrift erkannt wird. */
    val CREDIT_PREFIXES = listOf("Foto:", "Photo:", "Video:", "Credit:", "Bild:", "Quelle:", "Source:")

    fun build(before: String, media: List<UploadedMedia>, after: String): String =
        build(before, media.map { it to MediaCaption() }, after, "Foto:")

    fun build(before: String, media: List<Pair<UploadedMedia, MediaCaption>>, after: String, creditPrefix: String): String =
        (paragraphs(before) + media.map { (m, c) -> media(m, c, creditPrefix) } + paragraphs(after)).joinToString("\n\n")

    fun media(m: UploadedMedia, caption: MediaCaption = MediaCaption(), creditPrefix: String = "Foto:"): String =
        if (m.isVideo) video(m, captionHtml(caption, creditPrefix)) else image(m, captionHtml(caption, creditPrefix))

    /** HTML für <figcaption> (leer, wenn weder Unterschrift noch Credit). */
    fun captionHtml(caption: MediaCaption, creditPrefix: String): String {
        val lines = caption.caption.trim().lines().map { HtmlText.escape(it.trim()) }.filter { it.isNotEmpty() } +
            listOfNotNull(caption.credit.trim().takeIf { it.isNotEmpty() }?.let { HtmlText.escape("$creditPrefix $it") })
        return lines.joinToString("<br>")
    }

    /** Bildunterschrift in einem bestehenden Bild-/Video-Block ersetzen, alles andere bleibt unverändert. */
    fun replaceCaption(raw: String, captionHtml: String): String {
        val without = raw.replace(Regex("<figcaption\\b[^>]*>.*?</figcaption>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        if (captionHtml.isEmpty()) return without
        val end = without.lastIndexOf("</figure>")
        if (end < 0) return without
        return without.substring(0, end) + figcaption(captionHtml) + without.substring(end)
    }

    private fun figcaption(html: String) =
        if (html.isEmpty()) "" else "<figcaption class=\"wp-element-caption\">$html</figcaption>"

    /** Leerzeile = neuer Absatz, einfacher Umbruch = <br>. */
    fun paragraphs(text: String): List<String> =
        text.replace("\r\n", "\n")
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { p ->
                val html = p.lines().joinToString("<br>") { HtmlText.escape(it.trimEnd()) }
                "<!-- wp:paragraph -->\n<p>$html</p>\n<!-- /wp:paragraph -->"
            }

    fun heading(text: String, level: Int = 2): String {
        val l = level.coerceIn(1, 6)
        val attrs = if (l == 2) "" else " {\"level\":$l}"
        val html = HtmlText.escape(text.trim().replace(Regex("\\s*\\n\\s*"), " "))
        return "<!-- wp:heading$attrs -->\n<h$l class=\"wp-block-heading\">$html</h$l>\n<!-- /wp:heading -->"
    }

    fun image(m: UploadedMedia, captionHtml: String = ""): String {
        val slug = if (m.largeUrl != null) "large" else "full"
        val src = HtmlText.escape(m.largeUrl ?: m.sourceUrl)
        return "<!-- wp:image {\"id\":${m.id},\"sizeSlug\":\"$slug\",\"linkDestination\":\"none\"} -->\n" +
            "<figure class=\"wp-block-image size-$slug\"><img src=\"$src\" alt=\"\" class=\"wp-image-${m.id}\"/>${figcaption(captionHtml)}</figure>\n" +
            "<!-- /wp:image -->"
    }

    fun video(m: UploadedMedia, captionHtml: String = ""): String =
        "<!-- wp:video {\"id\":${m.id}} -->\n" +
            "<figure class=\"wp-block-video\"><video controls src=\"${HtmlText.escape(m.sourceUrl)}\"></video>${figcaption(captionHtml)}</figure>\n" +
            "<!-- /wp:video -->"
}

/** Optionale Bildunterschrift und Credit (z. B. Fotograf) zu einem Foto/Video. */
data class MediaCaption(val caption: String = "", val credit: String = "") {
    val isEmpty get() = caption.isBlank() && credit.isBlank()
}
