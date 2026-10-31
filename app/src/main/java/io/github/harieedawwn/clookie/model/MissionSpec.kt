package io.github.harieedawwn.clookie.model

import androidx.compose.runtime.Immutable

/**
 * One step of an alarm's mission chain: the mission type plus its difficulty settings
 * (integer values keyed by the mission's own setting keys, e.g. "count" -> 3).
 */
@Immutable
data class MissionSpec(val type: MissionType, val params: Map<String, Int> = emptyMap()) {
    fun param(key: String, default: Int): Int = params[key] ?: default
}

/**
 * Storage codec: "Classic;Math{level=2,count=3}". Unknown types (from a newer version) and
 * malformed values are dropped; an empty chain decodes to Classic so an alarm can always stop.
 */
object MissionCodec {
    fun encode(chain: List<MissionSpec>): String = chain.joinToString(";") { spec ->
        if (spec.params.isEmpty()) spec.type.name
        else spec.type.name + spec.params.entries.sortedBy { it.key }.joinToString(",", "{", "}") { "${it.key}=${it.value}" }
    }

    fun decode(text: String): List<MissionSpec> {
        val specs = text.split(';').mapNotNull { part ->
            val p = part.trim()
            if (p.isEmpty()) return@mapNotNull null
            val name = p.substringBefore('{')
            val type = MissionType.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            val params = p.substringAfter('{', "").substringBefore('}').split(',').mapNotNull { kv ->
                val k = kv.substringBefore('=', "").trim()
                val v = kv.substringAfter('=', "").trim().toIntOrNull()
                if (k.isEmpty() || v == null) null else k to v
            }.toMap()
            MissionSpec(type, params)
        }
        return specs.take(Alarm.MaxMissions).ifEmpty { listOf(MissionSpec(MissionType.Classic)) }
    }
}
