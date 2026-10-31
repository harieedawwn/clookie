package io.github.harieedawwn.clookie.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** A decoded code: ZXing format name and text, UPC-A folded into EAN-13. */
data class Decoded(val format: String, val value: String)

/**
 * Offline QR / barcode reading with ZXing core: pure Java, no Google Play services, no model
 * download. Only the formats people find at home: QR, retail barcodes (EAN/UPC) and Code 128.
 * Every other frame is read turned 90° so a barcode works whichever way the phone is held.
 */
class BarcodeReader {
    private val reader = MultiFormatReader().apply { setHints(Hints) }
    private var rotated = ByteArray(0)
    private var frame = 0

    fun decode(y: ByteArray, width: Int, height: Int, rowStride: Int = width): Decoded? {
        val turn = frame++ and 1 == 1
        val source = if (!turn) {
            PlanarYUVLuminanceSource(y, rowStride, height, 0, 0, width, height, false)
        } else {
            if (rotated.size != width * height) rotated = ByteArray(width * height)
            // 90° clockwise: new width = height.
            for (row in 0 until height) {
                val src = row * rowStride
                val col = height - 1 - row
                for (x in 0 until width) rotated[x * height + col] = y[src + x]
            }
            PlanarYUVLuminanceSource(rotated, height, width, 0, 0, height, width, false)
        }
        return try {
            val r = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            normalize(r.barcodeFormat, r.text)
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }

    companion object {
        val Formats = listOf(
            BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.CODE_128,
        )

        private val Hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to Formats,
            DecodeHintType.TRY_HARDER to true,
        )

        /** UPC-A is EAN-13 with a leading 0; store one form so either reading matches. */
        fun normalize(format: BarcodeFormat, text: String): Decoded =
            if (format == BarcodeFormat.UPC_A) Decoded(BarcodeFormat.EAN_13.name, "0$text") else Decoded(format.name, text)
    }
}
