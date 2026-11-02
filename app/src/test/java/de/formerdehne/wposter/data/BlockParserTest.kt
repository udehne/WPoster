package de.formerdehne.wposter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockParserTest {

    private val appPost = BlockBuilder.build(
        "Hallo Welt!\nZweite Zeile\n\nNeuer Absatz & mehr",
        listOf(
            UploadedMedia(11, "https://x.de/a.jpg", "https://x.de/a-1024.jpg", isImage = true, isVideo = false),
            UploadedMedia(12, "https://x.de/b.mp4", null, isImage = false, isVideo = true),
        ),
        "Tschüss <3",
    )

    @Test
    fun parsesOwnPostsRoundTrip() {
        val blocks = BlockParser.parse(appPost)
        assertEquals(5, blocks.size)
        val p1 = blocks[0] as ParsedBlock.Paragraph
        assertEquals("Hallo Welt!\nZweite Zeile", p1.text)
        assertFalse(p1.hasFormatting)
        assertEquals("Neuer Absatz & mehr", (blocks[1] as ParsedBlock.Paragraph).text)
        val img = blocks[2] as ParsedBlock.Image
        assertEquals(11L, img.id)
        assertEquals("https://x.de/a-1024.jpg", img.src)
        val video = blocks[3] as ParsedBlock.Video
        assertEquals(12L, video.id)
        assertEquals("https://x.de/b.mp4", video.src)
        assertEquals("Tschüss <3", (blocks[4] as ParsedBlock.Paragraph).text)
        // Unveränderte Blöcke ergeben wieder exakt denselben Inhalt.
        assertEquals(appPost, blocks.joinToString("\n\n") { it.raw })
    }

    @Test
    fun keepsNestedAndUnknownBlocks() {
        val content = """
            <!-- wp:heading {"level":3} -->
            <h3 class="wp-block-heading">Urlaub <strong>2026</strong></h3>
            <!-- /wp:heading -->

            <!-- wp:gallery {"linkTo":"none"} -->
            <figure class="wp-block-gallery"><!-- wp:image {"id":5} -->
            <figure class="wp-block-image"><img src="https://x.de/5.jpg" class="wp-image-5"/></figure>
            <!-- /wp:image -->

            <!-- wp:image {"id":6} -->
            <figure class="wp-block-image"><img src="https://x.de/6.jpg" class="wp-image-6"/></figure>
            <!-- /wp:image --></figure>
            <!-- /wp:gallery -->

            <!-- wp:separator /-->

            <!-- wp:paragraph -->
            <p>Mit <a href="https://x.de">Link</a></p>
            <!-- /wp:paragraph -->
        """.trimIndent()
        val blocks = BlockParser.parse(content)
        assertEquals(4, blocks.size)
        val h = blocks[0] as ParsedBlock.Heading
        assertEquals(3, h.level)
        assertEquals("Urlaub 2026", h.text)
        assertTrue(h.hasFormatting)
        val gallery = blocks[1] as ParsedBlock.Other
        assertEquals("gallery", gallery.blockName)
        assertEquals(2, gallery.imageCount)
        assertEquals("", gallery.summary)
        assertTrue(gallery.raw.endsWith("<!-- /wp:gallery -->"))
        assertEquals("separator", (blocks[2] as ParsedBlock.Other).blockName)
        val p = blocks[3] as ParsedBlock.Paragraph
        assertEquals("Mit Link", p.text)
        assertTrue(p.hasFormatting)
    }

    @Test
    fun classicContentBecomesOneBlock() {
        val blocks = BlockParser.parse("<p>Alter Beitrag</p>\n<img src=\"x.jpg\">")
        assertEquals(1, blocks.size)
        assertEquals(ParsedBlock.Other.FREEFORM, (blocks[0] as ParsedBlock.Other).blockName)
    }

    @Test
    fun emptyContent() {
        assertTrue(BlockParser.parse("").isEmpty())
        assertTrue(BlockParser.parse("   \n ").isEmpty())
    }

    @Test
    fun headingBuilderRoundTrip() {
        val raw = BlockBuilder.heading("Teil <2>", 2)
        val h = BlockParser.parse(raw).single() as ParsedBlock.Heading
        assertEquals("Teil <2>", h.text)
        assertEquals(2, h.level)
    }

    @Test
    fun captionAndCreditRoundTrip() {
        val m = UploadedMedia(7, "https://x.de/7.jpg", null, isImage = true, isVideo = false)
        val raw = BlockBuilder.media(m, MediaCaption("Am Strand\nmit Oma", "Uli & Simon"), "Foto:")
        assertTrue(raw.contains("<figcaption class=\"wp-element-caption\">Am Strand<br>mit Oma<br>Foto: Uli &amp; Simon</figcaption>"))
        val img = BlockParser.parse(raw).single() as ParsedBlock.Image
        assertEquals("Am Strand\nmit Oma", img.caption)
        assertEquals("Uli & Simon", img.credit)
        assertEquals(7L, img.id)
    }

    @Test
    fun replaceCaptionKeepsRestOfBlock() {
        val m = UploadedMedia(8, "https://x.de/8.jpg", "https://x.de/8-l.jpg", isImage = true, isVideo = false)
        val raw = BlockBuilder.media(m, MediaCaption("Alt"), "Foto:")
        val changed = BlockBuilder.replaceCaption(raw, BlockBuilder.captionHtml(MediaCaption("Neu", "Simon"), "Photo:"))
        val img = BlockParser.parse(changed).single() as ParsedBlock.Image
        assertEquals("Neu", img.caption)
        assertEquals("Simon", img.credit)
        val removed = BlockBuilder.replaceCaption(raw, "")
        assertFalse(removed.contains("figcaption"))
        assertTrue(removed.contains("wp-image-8"))
    }
}
