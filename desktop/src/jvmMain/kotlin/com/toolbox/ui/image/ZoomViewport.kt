package com.toolbox.ui.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max

class ZoomViewport {
    var zoom by mutableStateOf(1f)
    var panX by mutableStateOf(0f)
    var panY by mutableStateOf(0f)

    val zoomPercent: Int get() = (zoom * 100).toInt()

    fun resetToFit(boxW: Float, boxH: Float, imgW: Int, imgH: Int) {
        zoom = if (imgW > 0 && imgH > 0 && boxW > 0f && boxH > 0f) {
            minOf(boxW / imgW, boxH / imgH)
        } else {
            1f
        }
        panX = 0f
        panY = 0f
    }

    fun setZoom(
        newZoom: Float,
        anchor: Offset?,
        boxW: Float,
        boxH: Float,
        imgW: Int,
        imgH: Int
    ) {
        val target = newZoom.coerceIn(0.02f, 32f)
        if (abs(target - zoom) < 1e-6f) return
        val point = anchor ?: Offset(boxW / 2f, boxH / 2f)
        val before = layout(boxW, boxH, imgW, imgH)
        val imageX = before.toImageX(point.x)
        val imageY = before.toImageY(point.y)
        zoom = target
        val after = layout(boxW, boxH, imgW, imgH)
        panX += point.x - after.toDisplayX(imageX)
        panY += point.y - after.toDisplayY(imageY)
        clampPan(boxW, boxH, imgW, imgH)
    }

    fun zoomBy(
        factor: Float,
        anchor: Offset?,
        boxW: Float,
        boxH: Float,
        imgW: Int,
        imgH: Int
    ) {
        setZoom(zoom * factor, anchor, boxW, boxH, imgW, imgH)
    }

    fun panBy(dx: Float, dy: Float, boxW: Float, boxH: Float, imgW: Int, imgH: Int) {
        panX += dx
        panY += dy
        clampPan(boxW, boxH, imgW, imgH)
    }

    fun clampPan(boxW: Float, boxH: Float, imgW: Int, imgH: Int) {
        val rangeX = max(0f, (imgW * zoom - boxW) / 2f)
        val rangeY = max(0f, (imgH * zoom - boxH) / 2f)
        panX = panX.coerceIn(-rangeX, rangeX)
        panY = panY.coerceIn(-rangeY, rangeY)
    }

    fun layout(boxW: Float, boxH: Float, imgW: Int, imgH: Int): ImageIo.FitLayout {
        val dispW = imgW * zoom
        val dispH = imgH * zoom
        return ImageIo.FitLayout(
            scale = zoom,
            offsetX = (boxW - dispW) / 2f + panX,
            offsetY = (boxH - dispH) / 2f + panY,
            dispW = dispW,
            dispH = dispH
        )
    }
}
