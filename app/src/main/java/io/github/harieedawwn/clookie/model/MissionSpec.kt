package io.github.harieedawwn.clookie.model

import androidx.compose.runtime.Immutable
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * One step of an alarm's mission chain: the mission type plus its difficulty settings
 * (integer values keyed by the mission's own setting keys, e.g. "count" -> 3) and an optional
 * [text] (Typing: the user's own phrase).
 */
@Immutable
data class MissionSpec(val type: MissionType, val params: Map<String, Int> = emptyMap(), val text: String = "") {
    fun param(key: String, default: Int): Int = params[key] ?: default
}

/** Setting keys shared by missions and the sleeper-profile presets. */
object MissionKeys {
    const val Level = "level"
    const val Count = "count"
    const val Sensitivity = "sensitivity"
    const val Length = "length"
    const val Rounds = "rounds"
}

/**
 * Storage codec: "Classic;Math{count=3,level=2};Typing{count=1}~Ch%C3%A0o". Unknown types (from
 * a newer version) and malformed values are dropped; an empty chain decodes to Classic so an
 * alarm can always stop. The text is URL-encoded, so it never contains ';' or '~'.
 */
object MissionCodec {
    fun encode(chain: List<MissionSpec>): String = chain.joinToString(";") { spec ->
        val head = if (spec.params.isEmpty()) spec.type.name
        else spec.type.name + spec.params.entries.sortedBy { it.key }.joinToString(",", "{", "}") { "${it.key}=${it.value}" }
        if (spec.text.isEmpty()) head else head + "~" + URLEncoder.encode(spec.text, "UTF-8")
    }

    fun decode(text: String): List<MissionSpec> {
        val specs = text.split(';').mapNotNull { part ->
            val p = part.trim().substringBefore('~')
            if (p.isEmpty()) return@mapNotNull null
            val name = p.substringBefore('{')
            val type = MissionType.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            val params = p.substringAfter('{', "").substringBefore('}').split(',').mapNotNull { kv ->
                val k = kv.substringBefore('=', "").trim()
                val v = kv.substringAfter('=', "").trim().toIntOrNull()
                if (k.isEmpty() || v == null) null else k to v
            }.toMap()
            val extra = part.trim().substringAfter('~', "")
            MissionSpec(type, params, runCatching { URLDecoder.decode(extra, "UTF-8") }.getOrDefault(""))
        }
        return specs.take(Alarm.MaxMissions).ifEmpty { listOf(MissionSpec(MissionType.Classic)) }
    }
}
