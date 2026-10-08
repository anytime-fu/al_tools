package com.toolbox.util

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MosaicDetector {

    data class Region(val x: Int, val y: Int, val width: Int, val height: Int)

    private val BLOCK_SIZES = intArrayOf(6, 8, 12, 16, 20, 24, 32, 48, 64)
    private const val MAX_ANALYSIS_SIDE = 2048
    private const val MIN_CELLS = 4

    fun detect(image: BufferedImage, onStage: (String) -> Unit = {}): List<Region> {
        val scale = min(1.0, MAX_ANALYSIS_SIDE.toDouble() / max(image.width, image.height))
        val w = max(8, (image.width * scale).toInt())
        val h = max(8, (image.height * scale).toInt())
        val backScaleX = image.width.toDouble() / w
        val backScaleY = image.height.toDouble() / h

        onStage("分析网格…")
        val gray = extractGray(image, w, h)

        val gx = ByteArray(w * h)
        val gy = ByteArray(w * h)
        val colSum = LongArray(w)
        val rowSum = LongArray(h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val v = gray[i].toInt() and 0xFF
                val dx = if (x + 1 < w) min(255, abs((gray[i + 1].toInt() and 0xFF) - v)) else 0
                val dy = if (y + 1 < h) min(255, abs((gray[i + w].toInt() and 0xFF) - v)) else 0
                gx[i] = dx.toByte()
                gy[i] = dy.toByte()
                colSum[x] += dx.toLong()
                rowSum[y] += dy.toLong()
            }
        }

        var bestSize = 0
        var bestPhaseX = 0
        var bestPhaseY = 0
        var bestCells = BooleanArray(0)
        var bestCount = 0

        for (block in BLOCK_SIZES) {
            if (block * 3 > w || block * 3 > h) continue
            onStage("检测 ${block}px 网格…")
            val phaseX = bestPhase(colSum, block)
            val phaseY = bestPhase(rowSum, block)

            val cols = (w - phaseX) / block
            val rows = (h - phaseY) / block
            if (cols < 2 || rows < 2) continue
            val cells = BooleanArray(cols * rows)
            var count = 0
            for (cy in 0 until rows) {
                for (cx in 0 until cols) {
                    val x0 = phaseX + cx * block
                    val y0 = phaseY + cy * block
                    var seam = 0
                    var inner = 0
                    var seamN = 0
                    var innerN = 0
                    for (yy in y0 + 1 until y0 + block - 1) {
                        val rowBase = yy * w
                        seam += gx[rowBase + x0].toInt() and 0xFF
                        seam += gx[rowBase + x0 + block - 1].toInt() and 0xFF
                        seamN += 2
                        for (xx in x0 + 2 until x0 + block - 2) {
                            inner += gx[rowBase + xx].toInt() and 0xFF
                            innerN++
                        }
                    }
                    for (xx in x0 + 1 until x0 + block - 1) {
                        seam += gy[y0 * w + xx].toInt() and 0xFF
                        seam += gy[(y0 + block - 1) * w + xx].toInt() and 0xFF
                        seamN += 2
                        for (yy in y0 + 2 until y0 + block - 2) {
                            inner += gy[yy * w + xx].toInt() and 0xFF
                            innerN++
                        }
                    }
                    val seamMean = if (seamN > 0) seam.toFloat() / seamN else 0f
                    val innerMean = if (innerN > 0) inner.toFloat() / innerN else 0f
                    if (seamMean > 5f && seamMean - innerMean > 2.5f) {
                        cells[cy * cols + cx] = true
                        count++
                    }
                }
            }
            if (count > bestCount) {
                bestCount = count
                bestSize = block
                bestPhaseX = phaseX
                bestPhaseY = phaseY
                bestCells = cells
            }
        }

        if (bestSize == 0 || bestCount < MIN_CELLS) return emptyList()

        val cols = (w - bestPhaseX) / bestSize
        val rows = (h - bestPhaseY) / bestSize
        val visited = BooleanArray(bestCells.size)
        val regions = mutableListOf<Region>()
        for (i in bestCells.indices) {
            if (!bestCells[i] || visited[i]) continue
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = -1
            var maxY = -1
            val stack = ArrayDeque<Int>()
            stack.add(i)
            visited[i] = true
            var cellCount = 0
            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                val cx = idx % cols
                val cy = idx / cols
                cellCount++
                if (cx < minX) minX = cx
                if (cx > maxX) maxX = cx
                if (cy < minY) minY = cy
                if (cy > maxY) maxY = cy
                for (nc in neighbors(idx, cols, rows)) {
                    if (bestCells[nc] && !visited[nc]) {
                        visited[nc] = true
                        stack.add(nc)
                    }
                }
            }
            if (cellCount < MIN_CELLS) continue
            val pad = 1
            val cellX = max(0, minX - pad)
            val cellY = max(0, minY - pad)
            val cellX2 = min(cols - 1, maxX + pad)
            val cellY2 = min(rows - 1, maxY + pad)
            val rx = ((bestPhaseX + cellX * bestSize) * backScaleX).toInt()
            val ry = ((bestPhaseY + cellY * bestSize) * backScaleY).toInt()
            val rx2 = ((bestPhaseX + (cellX2 + 1) * bestSize) * backScaleX).toInt()
            val ry2 = ((bestPhaseY + (cellY2 + 1) * bestSize) * backScaleY).toInt()
            regions.add(
                Region(
                    x = rx.coerceIn(0, image.width - 1),
                    y = ry.coerceIn(0, image.height - 1),
                    width = (rx2 - rx).coerceAtMost(image.width - rx),
                    height = (ry2 - ry).coerceAtMost(image.height - ry)
                )
            )
        }
        return mergeRegions(regions)
    }

    private fun extractGray(image: BufferedImage, w: Int, h: Int): ByteArray {
        val src = if (image.width == w && image.height == h) {
            image
        } else {
            val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g: Graphics2D = scaled.createGraphics()
            g.drawImage(image, 0, 0, w, h, null)
            g.dispose()
            scaled
        }
        val pixels = src.getRGB(0, 0, w, h, null, 0, w)
        val gray = ByteArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = ((r + g + b) / 3).toByte()
        }
        return gray
    }

    private fun bestPhase(profile: LongArray, block: Int): Int {
        var best = 0
        var bestScore = Long.MIN_VALUE
        for (phase in 0 until block) {
            var sum = 0L
            var n = 0
            var idx = phase
            while (idx < profile.size) {
                sum += profile[idx]
                n++
                idx += block
            }
            val score = if (n > 0) sum else 0L
            if (score > bestScore) {
                bestScore = score
                best = phase
            }
        }
        return best
    }

    private fun neighbors(idx: Int, cols: Int, rows: Int): List<Int> {
        val cx = idx % cols
        val cy = idx / cols
        val out = ArrayList<Int>(4)
        if (cx > 0) out.add(idx - 1)
        if (cx < cols - 1) out.add(idx + 1)
        if (cy > 0) out.add(idx - cols)
        if (cy < rows - 1) out.add(idx + cols)
        return out
    }

    internal fun mergeRegions(regions: List<Region>): List<Region> {
        val result = regions.toMutableList()
        var merged = true
        while (merged) {
            merged = false
            outer@ for (i in result.indices) {
                for (j in i + 1 until result.size) {
                    if (intersectsOrNear(result[i], result[j])) {
                        result[i] = union(result[i], result[j])
                        result.removeAt(j)
                        merged = true
                        break@outer
                    }
                }
            }
        }
        return result
    }

    private fun intersectsOrNear(a: Region, b: Region): Boolean {
        val gap = 4
        return a.x - gap < b.x + b.width && b.x - gap < a.x + a.width &&
            a.y - gap < b.y + b.height && b.y - gap < a.y + a.height
    }

    private fun union(a: Region, b: Region): Region {
        val x = min(a.x, b.x)
        val y = min(a.y, b.y)
        val right = max(a.x + a.width, b.x + b.width)
        val bottom = max(a.y + a.height, b.y + b.height)
        return Region(x, y, right - x, bottom - y)
    }
}