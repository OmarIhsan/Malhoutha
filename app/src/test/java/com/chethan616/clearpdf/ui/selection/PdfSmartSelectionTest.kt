package com.chethan616.clearpdf.ui.selection

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.chethan616.clearpdf.ui.viewmodel.OcrTextBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSmartSelectionTest {

    @Test
    fun smartWordAtStripsTrailingPunctuationAndWhitespace() {
        val block = OcrTextBlock(
            id = "b1",
            text = "Hello, world! Welcome to Malhoutha; test: done.",
            left = 0.1f,
            top = 0.1f,
            right = 0.9f,
            bottom = 0.15f
        )
        val layout = PdfTextLayout(listOf(block))

        // "Hello," -> index 0..4 ("Hello")
        val w1 = layout.smartWordAt(2)
        assertEquals("Hello", layout.substring(w1.first, w1.last + 1))

        // "world!" -> index 7..11 ("world")
        val w2 = layout.smartWordAt(8)
        assertEquals("world", layout.substring(w2.first, w2.last + 1))

        // "Malhoutha;" -> "Malhoutha"
        val mIdx = layout.text.indexOf("Malhoutha")
        val w3 = layout.smartWordAt(mIdx + 3)
        assertEquals("Malhoutha", layout.substring(w3.first, w3.last + 1))

        // "done." -> "done"
        val dIdx = layout.text.indexOf("done")
        val w4 = layout.smartWordAt(dIdx + 1)
        assertEquals("done", layout.substring(w4.first, w4.last + 1))
    }

    @Test
    fun selectionRectsApplyTypographicPaddingAndAdjacentQuadMerging() {
        // Two adjacent words on the same line
        val block1 = OcrTextBlock(
            id = "b1",
            text = "First",
            left = 0.1f,
            top = 0.2f,
            right = 0.3f,
            bottom = 0.25f
        )
        val block2 = OcrTextBlock(
            id = "b2",
            text = "Second",
            left = 0.31f,
            top = 0.2f,
            right = 0.55f,
            bottom = 0.25f
        )
        val layout = PdfTextLayout(listOf(block1, block2))
        val pageSize = Size(1000f, 1500f)
        val density = 2f

        // Select across both blocks on the same line
        val rects = layout.selectionRects(0, layout.length, pageSize, density)

        // They must merge into 1 single unbroken rect on that line
        assertEquals(1, rects.size)

        val merged = rects[0]
        // Left should be expanded by 2.5dp (5px at density 2f)
        val rawLeft = 0.1f * pageSize.width
        val expectedLeft = (rawLeft - 2.5f * density).coerceAtLeast(0f)
        assertEquals(expectedLeft, merged.left, 1e-2f)

        // Right should be expanded by 3.5dp (7px at density 2f)
        val rawRight = 0.55f * pageSize.width
        val expectedRight = rawRight + 3.5f * density
        assertEquals(expectedRight, merged.right, 1e-2f)
    }

    @Test
    fun caretGeometryAlignsWithExpandedQuad() {
        val block = OcrTextBlock(
            id = "b1",
            text = "Sample",
            left = 0.2f,
            top = 0.3f,
            right = 0.6f,
            bottom = 0.35f
        )
        val layout = PdfTextLayout(listOf(block))
        val pageSize = Size(1000f, 1500f)
        val density = 2f

        val startCaret = layout.caretGeometry(0, isStart = true, size = pageSize, density = density)!!
        val endCaret = layout.caretGeometry(layout.length, isStart = false, size = pageSize, density = density)!!

        // Start caret x is expanded left by 2.5dp (5px)
        assertEquals(0.2f * 1000f - 2.5f * density, startCaret.x, 1e-2f)

        // End caret x is expanded right by 3.5dp (7px)
        assertEquals(0.6f * 1000f + 3.5f * density, endCaret.x, 1e-2f)

        // Line bottom includes 1.5dp expansion
        assertTrue(startCaret.lineBottom > 0.35f * 1500f)
    }
}
