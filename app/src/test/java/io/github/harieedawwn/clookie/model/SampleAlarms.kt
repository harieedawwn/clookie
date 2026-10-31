package io.github.harieedawwn.clookie.model

/** Fixture alarms for unit and screenshot tests (the app reads Room). */
object SampleAlarms {
    private fun m(vararg t: MissionType) = t.map { MissionSpec(it) }

    val list = listOf(
        Alarm(
            id = 1, hour = 6, minute = 30, days = Days.Weekdays, label = "Đi làm",
            missions = m(MissionType.CookieCode, MissionType.Math), profile = SleeperProfile.Deep,
            rampSeconds = 0, snoozeLimit = 0,
        ),
        Alarm(
            id = 2, hour = 8, minute = 15, days = Days.Weekend, label = "Cuối tuần",
            missions = m(MissionType.Classic), profile = SleeperProfile.Light, rampSeconds = 60, snoozeLimit = 5,
        ),
        Alarm(
            id = 3, hour = 13, minute = 5, days = Days.Weekdays, label = "Ngủ trưa",
            missions = m(MissionType.Crumbs), profile = SleeperProfile.Medium, enabled = false,
        ),
        Alarm(id = 4, hour = 21, minute = 45, label = "Uống thuốc", missions = m(MissionType.Classic)),
    )

    /** A long list for the 8-alarm screenshot. */
    val eight = (list + listOf(
        Alarm(id = 5, hour = 5, minute = 45, days = Days(0b0010101), label = "Chạy bộ", missions = m(MissionType.Steps)),
        Alarm(id = 6, hour = 7, minute = 0, days = Days.Every, label = "Thuốc sáng", vibration = VibrationPattern.Steady),
        Alarm(id = 7, hour = 9, minute = 30, days = Days.Weekend, label = "Chợ phiên", enabled = false),
        Alarm(id = 8, hour = 22, minute = 30, days = Days.Every, label = "Đi ngủ thôi", missions = m(MissionType.Classic, MissionType.Memory)),
    )).sortedBy { it.minutesOfDay }
}
