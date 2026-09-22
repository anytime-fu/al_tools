package com.toolbox.ui.armeasurement

import android.graphics.PointF
import kotlin.math.sqrt

object PerspectiveTransform {

    fun transform(src: List<PointF>, dst: List<PointF>, point: PointF): PointF {
        val matrix = computeMatrix(src, dst)
        return applyMatrix(matrix, point)
    }

    fun computeMatrix(src: List<PointF>, dst: List<PointF>): DoubleArray {
        val x0 = src[0].x.toDouble()
        val y0 = src[0].y.toDouble()
        val x1 = src[1].x.toDouble()
        val y1 = src[1].y.toDouble()
        val x2 = src[2].x.toDouble()
        val y2 = src[2].y.toDouble()
        val x3 = src[3].x.toDouble()
        val y3 = src[3].y.toDouble()

        val u0 = dst[0].x.toDouble()
        val v0 = dst[0].y.toDouble()
        val u1 = dst[1].x.toDouble()
        val v1 = dst[1].y.toDouble()
        val u2 = dst[2].x.toDouble()
        val v2 = dst[2].y.toDouble()
        val u3 = dst[3].x.toDouble()
        val v3 = dst[3].y.toDouble()

        val a = doubleArrayOf(
            x0, y0, 1.0, 0.0, 0.0, 0.0, -u0 * x0, -u0 * y0,
            0.0, 0.0, 0.0, x0, y0, 1.0, -v0 * x0, -v0 * y0,
            x1, y1, 1.0, 0.0, 0.0, 0.0, -u1 * x1, -u1 * y1,
            0.0, 0.0, 0.0, x1, y1, 1.0, -v1 * x1, -v1 * y1,
            x2, y2, 1.0, 0.0, 0.0, 0.0, -u2 * x2, -u2 * y2,
            0.0, 0.0, 0.0, x2, y2, 1.0, -v2 * x2, -v2 * y2,
            x3, y3, 1.0, 0.0, 0.0, 0.0, -u3 * x3, -u3 * y3,
            0.0, 0.0, 0.0, x3, y3, 1.0, -v3 * x3, -v3 * y3
        )

        val b = doubleArrayOf(u0, v0, u1, v1, u2, v2, u3, v3)

        val h = solveLinearSystem(a, b)

        return doubleArrayOf(
            h[0], h[1], h[2],
            h[3], h[4], h[5],
            h[6], h[7], 1.0
        )
    }

    private fun applyMatrix(m: DoubleArray, p: PointF): PointF {
        val x = p.x.toDouble()
        val y = p.y.toDouble()
        val w = m[6] * x + m[7] * y + m[8]
        val u = (m[0] * x + m[1] * y + m[2]) / w
        val v = (m[3] * x + m[4] * y + m[5]) / w
        return PointF(u.toFloat(), v.toFloat())
    }

    private fun solveLinearSystem(a: DoubleArray, b: DoubleArray): DoubleArray {
        val n = 8
        val aug = DoubleArray(n * (n + 1))

        for (i in 0 until n) {
            for (j in 0 until n) {
                aug[i * (n + 1) + j] = a[i * n + j]
            }
            aug[i * (n + 1) + n] = b[i]
        }

        for (col in 0 until n) {
            var maxRow = col
            for (row in col + 1 until n) {
                if (kotlin.math.abs(aug[row * (n + 1) + col]) > kotlin.math.abs(aug[maxRow * (n + 1) + col])) {
                    maxRow = row
                }
            }

            val tempRow = aug.copyOfRange(maxRow * (n + 1), (maxRow + 1) * (n + 1))
            for (j in 0..n) {
                aug[maxRow * (n + 1) + j] = aug[col * (n + 1) + j]
                aug[col * (n + 1) + j] = tempRow[j]
            }

            val pivot = aug[col * (n + 1) + col]
            if (kotlin.math.abs(pivot) < 1e-10) continue

            for (j in col..n) {
                aug[col * (n + 1) + j] /= pivot
            }

            for (row in 0 until n) {
                if (row != col) {
                    val factor = aug[row * (n + 1) + col]
                    for (j in col..n) {
                        aug[row * (n + 1) + j] -= factor * aug[col * (n + 1) + j]
                    }
                }
            }
        }

        val result = DoubleArray(n)
        for (i in 0 until n) {
            result[i] = aug[i * (n + 1) + n]
        }
        return result
    }

    fun realWorldDistance(
        p1: PointF,
        p2: PointF,
        cardCorners: List<PointF>,
        cardWidthMm: Float = 85.6f,
        cardHeightMm: Float = 53.98f
    ): Float {
        val realCard = listOf(
            PointF(0f, 0f),
            PointF(cardWidthMm, 0f),
            PointF(cardWidthMm, cardHeightMm),
            PointF(0f, cardHeightMm)
        )

        val rp1 = transform(cardCorners, realCard, p1)
        val rp2 = transform(cardCorners, realCard, p2)

        val dx = rp2.x - rp1.x
        val dy = rp2.y - rp1.y

        return sqrt(dx * dx + dy * dy) / 10f
    }
}
