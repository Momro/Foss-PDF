package dev.momro.fosspdf

import dev.momro.fosspdf.pdf.TextSpan
import dev.momro.fosspdf.pdf.buildTextRuns
import org.junit.Assert.assertEquals
import org.junit.Test

class TextLayerTest {

    @Test
    fun `spans on the same line merge into one run`() {
        val runs = buildTextRuns(
            listOf(
                TextSpan("H", 10f, 100f, 5f, 10f),
                TextSpan("i", 16f, 100f, 3f, 10f),
                TextSpan("!", 20f, 100f, 3f, 10f),
            )
        )
        assertEquals(1, runs.size)
        assertEquals("Hi!", runs[0].text)
        assertEquals(10f, runs[0].x)
        assertEquals(100f, runs[0].y)
        assertEquals(13f, runs[0].width)
        assertEquals(10f, runs[0].height)
    }

    @Test
    fun `horizontal gap inserts a space`() {
        val runs = buildTextRuns(
            listOf(
                TextSpan("a", 10f, 100f, 5f, 10f),
                TextSpan("b", 30f, 100f, 5f, 10f),
            )
        )
        assertEquals(1, runs.size)
        assertEquals("a b", runs[0].text)
        assertEquals(25f, runs[0].width)
    }

    @Test
    fun `different y values become separate runs sorted by y`() {
        val runs = buildTextRuns(
            listOf(
                TextSpan("b", 10f, 120f, 5f, 10f),
                TextSpan("a", 10f, 100f, 5f, 10f),
            )
        )
        assertEquals(2, runs.size)
        assertEquals("a", runs[0].text)
        assertEquals("b", runs[1].text)
    }

    @Test
    fun `out of order spans on a line are sorted by x`() {
        val runs = buildTextRuns(
            listOf(
                TextSpan("b", 15f, 100f, 5f, 10f),
                TextSpan("a", 10f, 100f, 5f, 10f),
            )
        )
        assertEquals(1, runs.size)
        assertEquals("ab", runs[0].text)
    }

    @Test
    fun `empty input returns no runs`() {
        assertEquals(emptyList<Any>(), buildTextRuns(emptyList()))
    }
}
