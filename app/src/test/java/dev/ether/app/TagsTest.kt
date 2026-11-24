package dev.ether.app

import dev.ether.app.domain.addTag
import dev.ether.app.domain.tagSuggestions
import org.junit.Assert.assertEquals
import org.junit.Test

class TagsTest {
    private val catalog = listOf("Kotlin", "Python", "PyTorch", "Compose")

    @Test
    fun `typed tags take the catalog spelling, custom ones stay as written, duplicates and blanks are skipped`() {
        assertEquals(listOf("Kotlin"), addTag(emptyList(), " kotlin ", catalog))
        assertEquals(listOf("Kotlin", "Ktor"), addTag(listOf("Kotlin"), "Ktor,", catalog))
        assertEquals(listOf("Kotlin"), addTag(listOf("Kotlin"), "KOTLIN", catalog))
        assertEquals(listOf("Kotlin"), addTag(listOf("Kotlin"), "  ", catalog))
    }

    @Test
    fun `suggestions match anywhere, prefix first, and leave out what is picked`() {
        assertEquals(listOf("Python", "PyTorch"), tagSuggestions("py", catalog, emptyList()))
        assertEquals(listOf("PyTorch"), tagSuggestions("py", catalog, listOf("python")))
        assertEquals(listOf("Compose", "Kotlin"), tagSuggestions("o", catalog, listOf("Python", "PyTorch")).sorted())
    }
}
