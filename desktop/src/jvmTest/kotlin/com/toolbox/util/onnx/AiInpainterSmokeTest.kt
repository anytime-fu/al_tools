package com.toolbox.util.onnx

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiInpainterSmokeTest {

    @Test
    fun inpaintFillsMaskedRegion() {
        val model = ModelManager.resolveModel()
        assertNotNull(model, "bundled model should resolve")

        val img = BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 96) {
            for (x in 0 until 96) {
                val v = if (x in 32 until 64 && y in 32 until 64) 0 else 255
                img.setRGB(x, y, (v shl 16) or (v shl 8) or v)
            }
        }
        val mask = MaskOps.create(96, 96)
        MaskOps.fillRect(mask, 32, 32, 32, 32, 255)

        val result = AiInpainter.inpaint(img, mask, model!!.absolutePath)

        assertEquals(96, result.width)
        val centerBefore = img.getRGB(48, 48) and 0xFF
        val centerAfter = result.getRGB(48, 48) and 0xFF
        val outsideBefore = img.getRGB(10, 10) and 0xFF
        val outsideAfter = result.getRGB(10, 10) and 0xFF

        assertEquals(outsideBefore, outsideAfter, "unmasked pixels must be untouched")
        assertTrue(centerAfter > centerBefore, "masked black square should be filled toward context (was $centerBefore now $centerAfter)")
    }

    @Test
    fun maskOpsBoundsAndCoverage() {
        val mask = MaskOps.create(64, 64)
        assertEquals(0, MaskOps.coverage(mask))
        MaskOps.fillRect(mask, 10, 12, 20, 8, 255)
        assertEquals(160, MaskOps.coverage(mask))
        val b = MaskOps.bounds(mask)
        assertNotNull(b)
        assertEquals(10, b.x)
        assertEquals(12, b.y)
        assertEquals(20, b.width)
        assertEquals(8, b.height)
    }

    @Test
    fun emptyMaskReturnsOriginal() {
        val model = ModelManager.resolveModel() ?: return
        val img = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        val mask = MaskOps.create(32, 32)
        val result = AiInpainter.inpaint(img, mask, model.absolutePath)
        assertEquals(img.getRGB(5, 5), result.getRGB(5, 5))
    }
}