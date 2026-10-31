package io.github.harieedawwn.clookie.model

/**
 * Vietnamese word clock on the 24-hour day: "bảy giờ đúng", "bảy giờ mười lăm", "bảy giờ rưỡi",
 * "bảy giờ kém mười lăm" (:45-:59 count down to the next hour, 23:50 -> "không giờ kém mười").
 * Minutes below ten take "phút" ("bảy giờ năm phút", "bảy giờ kém năm phút") so they never read
 * like an hour.
 */
object WordClock {
    private val ones = arrayOf("không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")

    /** Natural number words 0-99: mười một, hai mươi mốt, hai mươi tư, mười lăm, ba mươi. */
    fun number(n: Int): String {
        require(n in 0..99)
        if (n < 10) return ones[n]
        val tens = n / 10
        val unit = n % 10
        val head = if (tens == 1) "mười" else ones[tens] + " mươi"
        val tail = when (unit) {
            0 -> return head
            1 -> if (tens == 1) "một" else "mốt"
            4 -> if (tens == 1) "bốn" else "tư"
            5 -> "lăm"
            else -> ones[unit]
        }
        return "$head $tail"
    }

    private fun minutes(m: Int) = if (m < 10) number(m) + " phút" else number(m)

    fun vi(hour: Int, minute: Int): String {
        require(hour in 0..23 && minute in 0..59)
        return when {
            minute == 0 -> "${number(hour)} giờ đúng"
            minute == 30 -> "${number(hour)} giờ rưỡi"
            minute >= 45 -> "${number((hour + 1) % 24)} giờ kém ${minutes(60 - minute)}"
            else -> "${number(hour)} giờ ${minutes(minute)}"
        }
    }
}
