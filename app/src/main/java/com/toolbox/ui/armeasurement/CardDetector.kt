package com.toolbox.ui.armeasurement

import android.graphics.PointF

object CardDetector {
    const val CARD_WIDTH_MM = 85.6f
    const val CARD_HEIGHT_MM = 53.98f

    fun calculateDistance(
        p1: PointF,
        p2: PointF,
        cardCorners: List<PointF>
    ): Float {
        if (cardCorners.size < 4) return 0f
        return PerspectiveTransform.realWorldDistance(p1, p2, cardCorners)
    }
}
