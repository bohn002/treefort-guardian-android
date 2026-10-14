package com.treefort.guardian

import com.treefort.guardian.ui.HelpContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HelpContentTest {
    // Unit tests run with the app module as working dir; the shared file lives in the meta repo.
    private val text = File("../../docs/help_content.json").readText()

    @Test fun `shared help content parses with categories and faqs`() {
        val c = HelpContent.parse(text)
        assertTrue(c.version >= 1)
        assertTrue(c.categories.isNotEmpty())
        assertTrue(c.faqs.isNotEmpty())
    }

    @Test fun `every faq category exists and ids are unique`() {
        val c = HelpContent.parse(text)
        val cats = c.categories.map { it.id }.toSet()
        assertTrue(c.faqs.all { it.category in cats })
        assertEquals(c.faqs.size, c.faqs.map { it.id }.toSet().size)
    }

    @Test fun `price mentions are 4_49 per month`() {
        val prices = Regex("""\$\d+(\.\d+)?""").findAll(text).map { it.value }.toSet()
        assertEquals(setOf("$4.49"), prices)
    }
}
