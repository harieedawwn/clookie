package io.github.harieedawwn.clookie.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.harieedawwn.clookie.model.CookieCode
import io.github.harieedawwn.clookie.model.CookieDetector
import io.github.harieedawwn.clookie.model.CookiePattern
import io.github.harieedawwn.clookie.model.ScanCode
import io.github.harieedawwn.clookie.model.ScanGate

/** Receives camera luminance frames on the analysis thread. */
fun interface FrameSink {
    fun onFrame(y: ByteArray, width: Int, height: Int, rowStride: Int)
}

/**
 * Base for the per-frame scanners. State is written from the analysis thread (snapshot state
 * is thread-safe; unchanged values are not written) and read by the scan UI.
 */
abstract class Scanner : FrameSink {
    /** Consecutive frames agreeing on a wanted code, up to [required]. */
    var streak by mutableIntStateOf(0)
        protected set

    /** Another code (not a wanted one) has been in view for a few frames. */
    var wrong by mutableStateOf(false)
        protected set

    /** The scene has been dark for a while: time for the torch. */
    var dark by mutableStateOf(false)
        protected set

    /** Id of the accepted code, or [ScanGate.NoMatch] while scanning. */
    var accepted by mutableLongStateOf(ScanGate.NoMatch)
        protected set

    abstract val required: Int

    private var darkFrames = 0

    protected fun luma(mean: Int) {
        darkFrames = if (mean < DarkLuma) darkFrames + 1 else 0
        if (darkFrames >= 8) dark = true else if (mean > DarkLuma + 25) dark = false
    }

    companion object {
        const val DarkLuma = 45
    }
}

/**
 * Cookie Code scanning: accepts one of [targets] after 3 consecutive matching frames.
 * Allocation-free per frame. [targets] may change from the UI thread.
 */
class CookieScanner(targets: List<CookieCode>) : Scanner() {
    override val required = 3
    private val detector = CookieDetector()
    private val gate = ScanGate(required)
    @Volatile private var ids = LongArray(0)
    @Volatile private var patterns = IntArray(0)
    private var wrongFrames = 0

    init {
        setTargets(targets)
    }

    fun setTargets(targets: List<CookieCode>) {
        val p = IntArray(targets.size) { targets[it].pattern }
        ids = LongArray(targets.size) { targets[it].id }
        patterns = p
        gate.reset()
    }

    override fun onFrame(y: ByteArray, width: Int, height: Int, rowStride: Int) {
        if (accepted != ScanGate.NoMatch) return
        detector.detect(y, width, height, rowStride)
        luma(detector.meanLuma)
        val ids = ids
        val patterns = patterns
        var match = ScanGate.NoMatch
        var other = false
        for (i in 0 until detector.count) {
            val read = detector.patterns[i]
            var found = -1
            for (k in patterns.indices) if (CookiePattern.matches(read, patterns[k])) found = k
            if (found >= 0) match = ids[found] else if (Integer.bitCount(read) in 1..8) other = true
        }
        val done = gate.feed(match)
        streak = gate.streak
        wrongFrames = if (other && match == ScanGate.NoMatch) wrongFrames + 1 else 0
        wrong = wrongFrames >= 3
        if (done) accepted = match
    }
}

/**
 * QR / barcode scanning. With [targets] it accepts a registered code; without, it accepts any
 * code (registration) and exposes it as [found]. A code must be read twice within 4 frames
 * (barcodes are read on every other frame, see [BarcodeReader]).
 */
class BarcodeScanner(targets: List<ScanCode>?) : Scanner() {
    override val required = 2
    private val reader = BarcodeReader()
    @Volatile private var targets: List<ScanCode>? = targets
    private var last: Decoded? = null
    private var hits = 0
    private var idle = 0
    private var wrongFrames = 0

    /** The code accepted in registration mode. */
    var found by mutableStateOf<Decoded?>(null)
        private set

    fun setTargets(targets: List<ScanCode>?) {
        this.targets = targets
    }

    /** Scan again after a registration was shown. */
    fun restart() {
        found = null
        accepted = ScanGate.NoMatch
        last = null
        hits = 0
        streak = 0
    }

    override fun onFrame(y: ByteArray, width: Int, height: Int, rowStride: Int) {
        if (accepted != ScanGate.NoMatch || found != null) return
        luma(sampleMean(y, width, height, rowStride))
        val d = reader.decode(y, width, height, rowStride)
        if (d == null) {
            if (++idle >= 4) {
                hits = 0
                last = null
                streak = 0
            }
            if (idle >= 4) wrongFrames = 0
            wrong = wrongFrames >= 2
            return
        }
        idle = 0
        hits = if (d == last) hits + 1 else 1
        last = d
        val targets = targets
        if (targets == null) {
            streak = hits.coerceAtMost(required)
            if (hits >= required) {
                found = d
                accepted = 0
            }
            return
        }
        val match = targets.firstOrNull { it.format == d.format && it.value == d.value }
        if (match == null) {
            wrongFrames++
            streak = 0
        } else {
            wrongFrames = 0
            streak = hits.coerceAtMost(required)
            if (hits >= required) accepted = match.id
        }
        wrong = wrongFrames >= 2
    }

    private fun sampleMean(y: ByteArray, width: Int, height: Int, stride: Int): Int {
        var sum = 0L
        var n = 0
        var row = 0
        while (row < height) {
            var x = 0
            while (x < width) {
                sum += y[row * stride + x].toInt() and 0xFF
                n++
                x += 8
            }
            row += 8
        }
        return if (n == 0) 0 else (sum / n).toInt()
    }
}
