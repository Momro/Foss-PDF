package dev.momro.fosspdf

import dev.momro.fosspdf.pdf.findMatchIndices
import org.junit.Assert.assertEquals
import org.junit.Test

class TextMatchTest {

    @Test
    fun `finds all matches case insensitive`() {
        assertEquals(listOf(0, 5, 10), findMatchIndices("Test test TEST", "test"))
    }

    @Test
    fun `empty query returns nothing`() {
        assertEquals(emptyList<Int>(), findMatchIndices("abc", ""))
    }

    @Test
    fun `no match returns empty list`() {
        assertEquals(emptyList<Int>(), findMatchIndices("hello world", "xyz"))
    }

    @Test
    fun `matches are not double counted`() {
        assertEquals(listOf(0), findMatchIndices("aaa", "aa"))
    }
}
