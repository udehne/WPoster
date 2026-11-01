package de.formerdehne.wposter.data

/** Erzeugt Inhalte im Format des Block-Editors (Gutenberg). */
object BlockBuilder {

    fun build(before: String, media: List<UploadedMedia>, after: String): String =
        (paragraphs(before) + media.map { media(it) } + paragraphs(after)).joinToString("\n\n")

    fun media(m: UploadedMedia): String = if (m.isVideo) video(m) else image(m)

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

    fun image(m: UploadedMedia): String {
        val slug = if (m.largeUrl != null) "large" else "full"
        val src = HtmlText.escape(m.largeUrl ?: m.sourceUrl)
        return "<!-- wp:image {\"id\":${m.id},\"sizeSlug\":\"$slug\",\"linkDestination\":\"none\"} -->\n" +
            "<figure class=\"wp-block-image size-$slug\"><img src=\"$src\" alt=\"\" class=\"wp-image-${m.id}\"/></figure>\n" +
            "<!-- /wp:image -->"
    }

    fun video(m: UploadedMedia): String =
        "<!-- wp:video {\"id\":${m.id}} -->\n" +
            "<figure class=\"wp-block-video\"><video controls src=\"${HtmlText.escape(m.sourceUrl)}\"></video></figure>\n" +
            "<!-- /wp:video -->"
}
