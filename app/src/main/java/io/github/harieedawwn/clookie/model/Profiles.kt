package io.github.harieedawwn.clookie.model

/**
 * Sleeper profiles: one-tap presets over the alarm's wake fields; everything stays editable and
 * an alarm shows a profile only while its fields still match it.
 */
object SleeperProfiles {
    val MediumMath = MissionSpec(MissionType.Math, mapOf(MissionKeys.Level to 2, MissionKeys.Count to 3))
    val MediumCrumbs = MissionSpec(
        MissionType.Crumbs,
        mapOf(MissionKeys.Level to MissionTuning.Crumbs.DefaultLevel, MissionKeys.Count to MissionTuning.Crumbs.DefaultCount),
    )
    val DeepChain = listOf(MissionSpec(MissionType.CookieCode), MediumMath)

    /** Applies [profile]; Ngủ vừa uses Crumbs instead of math when [crumbs]. */
    fun apply(alarm: Alarm, profile: SleeperProfile, crumbs: Boolean = false): Alarm = when (profile) {
        SleeperProfile.Light -> alarm.copy(
            missions = listOf(MissionSpec(MissionType.Classic)), rampSeconds = 60, snoozeMinutes = 9, snoozeLimit = 5,
            volumeLock = false, snoozeAfterMission = false, awakeCheck = false, profile = profile,
        )
        SleeperProfile.Medium -> alarm.copy(
            missions = listOf(if (crumbs) MediumCrumbs else MediumMath), rampSeconds = 20, snoozeLimit = 2,
            volumeLock = false, snoozeAfterMission = false, awakeCheck = false, profile = profile,
        )
        SleeperProfile.Deep -> alarm.copy(
            missions = DeepChain, volume = 100, rampSeconds = 0,
            volumeLock = true, snoozeAfterMission = true, awakeCheck = true, awakeCheckMinutes = AwakeRules.DefaultMinutes,
            profile = profile,
        )
    }

    /** The profile the alarm's fields still match (code choice and other settings may differ), or null. */
    fun matching(alarm: Alarm): SleeperProfile? = SleeperProfile.entries.firstOrNull { p ->
        when (p) {
            SleeperProfile.Light -> alarm.missionTypes == listOf(MissionType.Classic) && alarm.rampSeconds == 60 &&
                alarm.snoozeMinutes == 9 && alarm.snoozeLimit == 5 && !alarm.volumeLock && !alarm.snoozeAfterMission && !alarm.awakeCheck
            SleeperProfile.Medium -> (alarm.missions == listOf(MediumMath) || alarm.missions == listOf(MediumCrumbs)) &&
                alarm.rampSeconds == 20 && alarm.snoozeLimit == 2 && !alarm.volumeLock && !alarm.snoozeAfterMission && !alarm.awakeCheck
            SleeperProfile.Deep -> alarm.missionTypes == DeepChain.map { it.type } && alarm.missions[1] == MediumMath &&
                alarm.volume == 100 && alarm.rampSeconds == 0 && alarm.volumeLock && alarm.snoozeAfterMission && alarm.awakeCheck
        }
    }
}
