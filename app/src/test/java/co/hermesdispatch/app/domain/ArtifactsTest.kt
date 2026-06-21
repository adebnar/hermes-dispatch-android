package co.hermesdispatch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactsTest {

    @Test
    fun `extracts a single url`() {
        val a = Artifacts.extract("See https://example.com/sheet for details.")
        assertEquals(1, a.size)
        assertEquals("https://example.com/sheet", a.first().url)
    }

    @Test
    fun `trims trailing punctuation`() {
        val a = Artifacts.extract("Here: https://docs.google.com/spreadsheets/d/abc.")
        assertEquals("https://docs.google.com/spreadsheets/d/abc", a.first().url)
    }

    @Test
    fun `dedupes preserving order`() {
        val a = Artifacts.extract("https://a.com then https://b.com then https://a.com")
        assertEquals(listOf("https://a.com", "https://b.com"), a.map { it.url })
    }

    @Test
    fun `flags image urls`() {
        assertTrue(Artifacts.extract("https://x.com/p/photo.PNG").first().isImage)
        assertTrue(Artifacts.extract("https://x.com/p/photo.jpg?w=1").first().isImage)
        assertFalse(Artifacts.extract("https://x.com/page").first().isImage)
    }

    @Test
    fun `blank text yields nothing`() {
        assertTrue(Artifacts.extract("   ").isEmpty())
    }
}
