package com.ephemeris.helios.utils.charts

data class ChartData(
    val xValues: FloatArray,
    val yValues: FloatArray,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val width: Float,
    val height: Float,
    val verticalPaddingPx: Float,
    val topPaddingPx: Float = verticalPaddingPx,
    val bottomPaddingPx: Float = verticalPaddingPx,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartData

        if (minX != other.minX) return false
        if (maxX != other.maxX) return false
        if (minY != other.minY) return false
        if (maxY != other.maxY) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!xValues.contentEquals(other.xValues)) return false
        if (!yValues.contentEquals(other.yValues)) return false
        if (verticalPaddingPx != other.verticalPaddingPx) return false
        if (topPaddingPx != other.topPaddingPx) return false
        if (bottomPaddingPx != other.bottomPaddingPx) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minX.hashCode()
        result = 31 * result + maxX.hashCode()
        result = 31 * result + minY.hashCode()
        result = 31 * result + maxY.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + xValues.contentHashCode()
        result = 31 * result + yValues.contentHashCode()
        result = 31 * result + verticalPaddingPx.hashCode()
        result = 31 * result + topPaddingPx.hashCode()
        result = 31 * result + bottomPaddingPx.hashCode()
        return result
    }
}