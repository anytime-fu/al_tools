package com.toolbox.util

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MosaicDetectorTest {

    private fun smoothGradientImage(): BufferedImage {
        val img = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val v = 90 + (x * 40 / 256)
                img.setRGB(x, y, (v shl 16) or (v shl 8) or v)
            }
        }
        return img
    }

    private fun mosaicImage(blockStart: Int, block: Int, blocks: Int): BufferedImage {
        val img = smoothGradientImage()
        for (by in 0 until blocks) {
            for (bx in 0 until blocks) {
                val base = 100 + (bx * 3) + (by * 2)
                val tint = if ((bx + by) % 2 == 0) 24 else -24
                val v = (base + tint).coerceIn(0, 255)
                for (dy in 0 until block) {
                    for (dx in 0 until block) {
                        img.setRGB(
                            blockStart + bx * block + dx,
                            blockStart + by * block + dy,
                            (v shl 16) or (v shl 8) or v
                        )
                    }
                }
            }
        }
        return img
    }

    @Test
    fun detectsAlignedMosaicPatch() {
        val img = mosaicImage(blockStart = 96, block = 16, blocks = 4)
        val regions = MosaicDetector.detect(img)
        assertTrue(regions.isNotEmpty(), "should detect mosaic patch")
        val patch = MosaicDetector.Region(96, 96, 64, 64)
        val overlapping = regions.any { r ->
            r.x < patch.x + patch.width && patch.x < r.x + r.width &&
                r.y < patch.y + patch.height && patch.y < r.y + r.height
        }
        assertTrue(overlapping, "detected region should overlap the mosaic patch: $regions")
    }

    @Test
    fun detectsMisalignedMosaicPatch() {
        val img = mosaicImage(blockStart = 101, block = 16, blocks = 4)
        val regions = MosaicDetector.detect(img)
        assertTrue(regions.isNotEmpty(), "phase-adaptive detection should find misaligned grid")
    }

    @Test
    fun smoothImageYieldsNoRegions() {
        val regions = MosaicDetector.detect(smoothGradientImage())
        assertTrue(regions.isEmpty(), "smooth image should have no regions, got $regions")
    }

    @Test
    fun mergeRegionsCombinesOverlapping() {
        val a = MosaicDetector.Region(0, 0, 10, 10)
        val b = MosaicDetector.Region(8, 8, 10, 10)
        val merged = MosaicDetector.mergeRegions(listOf(a, b))
        assertEquals(1, merged.size)
        assertEquals(MosaicDetector.Region(0, 0, 18, 18), merged.first())
    }
}