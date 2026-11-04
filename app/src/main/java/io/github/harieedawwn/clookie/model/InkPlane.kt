package io.github.harieedawwn.clookie.model

/**
 * Blue ink counts as dark: light blue / cyan felt pens are nearly as bright as the paper in
 * luminance alone, so the detector would see no marker. Each luminance sample is lowered by
 * [BlueWeight] x how blue its chroma is (Cb above neutral). Grey printed grid lines, black and
 * red ink have no blue chroma and stay as they are. In place, allocation-free.
 */
object InkPlane {
    const val BlueWeight = 1.5f

    /**
     * [y]: luminance rows of [yStride]; [u]: the half-resolution Cb plane with [uRowStride] and
     * [uPixelStride] (YUV_420_888 layout).
     */
    fun darkenBlue(y: ByteArray, yStride: Int, width: Int, height: Int, u: ByteArray, uRowStride: Int, uPixelStride: Int) {
        for (row in 0 until height) {
            val yRow = row * yStride
            val uRow = (row shr 1) * uRowStride
            for (x in 0 until width) {
                val blue = (u[uRow + (x shr 1) * uPixelStride].toInt() and 0xFF) - 128
                if (blue <= 0) continue
                val i = yRow + x
                val v = (y[i].toInt() and 0xFF) - (BlueWeight * blue).toInt()
                y[i] = (if (v < 0) 0 else v).toByte()
            }
        }
    }
}
