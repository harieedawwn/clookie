package io.github.harieedawwn.clookie.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** ZXing reading on synthetic 640x480 camera frames: rotated, blurred by sampling, noisy. */
class BarcodeReaderTest {
    private val w = 640
    private val h = 480

    /** Draws [m] centered, [scale] px per module, rotated [deg], on paper 200 with ink 40 and noise. */
    private fun frame(m: BitMatrix, scale: Double, deg: Double, rnd: Random): ByteArray {
        val out = ByteArray(w * h)
        val a = Math.toRadians(deg)
        val cx = w / 2.0
        val cy = h / 2.0
        for (y in 0 until h) for (x in 0 until w) {
            // Inverse rotate into code space; 2x2 supersampling.
            var sum = 0.0
            for (sy in 0..1) for (sx in 0..1) {
                val px = x + 0.25 + sx * 0.5 - cx
                val py = y + 0.25 + sy * 0.5 - cy
                val u = (px * cos(a) + py * sin(a)) / scale + m.width / 2.0
                val v = (-px * sin(a) + py * cos(a)) / scale + m.height / 2.0
                val ink = u >= 0 && v >= 0 && u < m.width && v < m.height && m.get(u.toInt(), v.toInt())
                sum += if (ink) 40.0 else 200.0
            }
            out[y * w + x] = (sum / 4 + (rnd.nextDouble() - 0.5) * 16).roundToInt().coerceIn(0, 255).toByte()
        }
        return out
    }

    private fun read(frame: ByteArray): Decoded? {
        val reader = BarcodeReader()
        // Two frames: as-is and turned (the reader alternates).
        return reader.decode(frame, w, h) ?: reader.decode(frame, w, h)
    }

    @Test
    fun readsTiltedQr() {
        val m = MultiFormatWriter().encode("WIFI:S:Clookie;T:WPA;;", BarcodeFormat.QR_CODE, 0, 0)
        assertEquals(Decoded("QR_CODE", "WIFI:S:Clookie;T:WPA;;"), read(frame(m, 7.0, 25.0, Random(1))))
    }

    @Test
    fun readsProductBarcodeHeldEitherWay() {
        val m = MultiFormatWriter().encode("8934588012112", BarcodeFormat.EAN_13, 0, 60)
        for (deg in listOf(4.0, 92.0)) assertEquals(Decoded("EAN_13", "8934588012112"), read(frame(m, 3.5, deg, Random(2))))
    }

    @Test
    fun upcAIsStoredAsEan13() {
        val m = MultiFormatWriter().encode("036000291452", BarcodeFormat.UPC_A, 0, 60)
        assertEquals(Decoded("EAN_13", "0036000291452"), read(frame(m, 3.5, 0.0, Random(3))))
    }

    @Test
    fun blankPaperReadsNothing() {
        val m = BitMatrix(1, 1)
        assertNull(read(frame(m, 1.0, 0.0, Random(4))))
    }
}
