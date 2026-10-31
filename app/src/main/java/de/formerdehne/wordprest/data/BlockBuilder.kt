package de.formerdehne.wordprest.data

/** Erzeugt Inhalte im Format des Block-Editors (Gutenberg). */
object BlockBuilder {

    fun build(before: String, media: List<UploadedMedia>, after: String): String = buildString {
        paragraphs(before).forEach { append(it) }
        media.forEach { append(if (it.isVideo) video(it) else image(it)) }
        paragraphs(after).forEach { append(it) }
    }.trim()

    private fun paragraphs(text: String): List<String> =
        text.replace("\r\n", "\n")
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { p ->
                val html = p.lines().joinToString("<br>") { escape(it) }
                "<!-- wp:paragraph -->\n<p>$html</p>\n<!-- /wp:paragraph -->\n\n"
            }

    private fun image(m: UploadedMedia): String {
        val slug = if (m.largeUrl != null) "large" else "full"
        val src = escape(m.largeUrl ?: m.sourceUrl)
        return "<!-- wp:image {\"id\":${m.id},\"sizeSlug\":\"$slug\",\"linkDestination\":\"none\"} -->\n" +
            "<figure class=\"wp-block-image size-$slug\"><img src=\"$src\" alt=\"\" class=\"wp-image-${m.id}\"/></figure>\n" +
            "<!-- /wp:image -->\n\n"
    }

    private fun video(m: UploadedMedia): String =
        "<!-- wp:video {\"id\":${m.id}} -->\n" +
            "<figure class=\"wp-block-video\"><video controls src=\"${escape(m.sourceUrl)}\"></video></figure>\n" +
            "<!-- /wp:video -->\n\n"

    private fun escape(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
