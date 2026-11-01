package de.formerdehne.wposter.data

/** Kleine HTML-Helfer ohne Android-Abhängigkeiten (damit unit-testbar). */
object HtmlText {

    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "hellip" to "…", "ndash" to "–", "mdash" to "—", "lsquo" to "‘", "rsquo" to "’", "sbquo" to "‚",
        "ldquo" to "“", "rdquo" to "”", "bdquo" to "„", "laquo" to "«", "raquo" to "»", "euro" to "€",
        "copy" to "©", "reg" to "®", "times" to "×", "auml" to "ä", "ouml" to "ö", "uuml" to "ü",
        "Auml" to "Ä", "Ouml" to "Ö", "Uuml" to "Ü", "szlig" to "ß",
    )
    private val entity = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);")
    private val br = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]*>")

    fun decode(s: String): String = entity.replace(s) { m ->
        val e = m.groupValues[1]
        val code = when {
            e.startsWith("#x") || e.startsWith("#X") -> e.substring(2).toIntOrNull(16)
            e.startsWith("#") -> e.substring(1).toIntOrNull()
            else -> null
        }
        if (code != null) {
            try { String(Character.toChars(code)) } catch (_: IllegalArgumentException) { m.value }
        } else {
            named[e] ?: m.value
        }
    }

    /** HTML → Klartext: <br> wird Zeilenumbruch, übrige Tags fallen weg. */
    fun toPlain(html: String): String =
        decode(html.replace(br, "\n").replace(tag, "")).replace(' ', ' ')

    fun escape(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /** Enthält das HTML Formatierungen außer Zeilenumbrüchen (Links, fett, …)? */
    fun hasFormatting(html: String): Boolean =
        Regex("<(?!br\\b|/br\\b)[a-zA-Z/]").containsMatchIn(html)
}
