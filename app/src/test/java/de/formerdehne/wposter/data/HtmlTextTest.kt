package de.formerdehne.wposter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTextTest {
    @Test
    fun decodesEntities() {
        assertEquals("Tom & Jerry – „hallo“ ’s", HtmlText.decode("Tom &amp; Jerry &#8211; &bdquo;hallo&ldquo; &#x2019;s"))
        assertEquals("&unknown;", HtmlText.decode("&unknown;"))
    }

    @Test
    fun toPlainKeepsLineBreaks() {
        assertEquals("a\nb c", HtmlText.toPlain("a<br>b <em>c</em>"))
    }

    @Test
    fun detectsFormatting() {
        assertFalse(HtmlText.hasFormatting("Zeile<br>Zeile<br/>"))
        assertTrue(HtmlText.hasFormatting("<strong>fett</strong>"))
    }
}
