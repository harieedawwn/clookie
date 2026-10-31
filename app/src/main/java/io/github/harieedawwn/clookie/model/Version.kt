package io.github.harieedawwn.clookie.model

/** "v0.2.0" / "0.2.0" -> [0, 2, 0]; null when not a dotted number. */
fun parseVersion(text: String): List<Int>? {
    val parts = text.trim().removePrefix("v").removePrefix("V").split('.')
    if (parts.isEmpty()) return null
    return parts.map { it.toIntOrNull() ?: return null }
}

/** True when [remote] is a strictly higher version than [local]. */
fun isNewerVersion(remote: String, local: String): Boolean {
    val r = parseVersion(remote) ?: return false
    val l = parseVersion(local) ?: return true
    for (i in 0 until maxOf(r.size, l.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = l.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
