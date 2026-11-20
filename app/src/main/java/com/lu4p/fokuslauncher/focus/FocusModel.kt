package com.lu4p.fokuslauncher.focus

import com.lu4p.fokuslauncher.home.layout.LayoutPreset

/** Fixed profile ids. Each exists exactly once; the user edits them in place. */
enum class FocusKind(val id: String) {
    STUDY("study"),
    WORK("work"),
    PERSONAL("personal"),
    SLEEP("sleep"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String): FocusKind? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Days use ISO numbers (1 = Monday ... 7 = Sunday). Minutes are minutes after local midnight.
 * An end earlier than the start means the range runs past midnight and belongs to the start day.
 */
data class FocusSchedule(
        val enabled: Boolean = false,
        val days: Set<Int> = emptySet(),
        val startMinute: Int = 9 * 60,
        val endMinute: Int = 17 * 60,
) {
    fun isActiveAt(isoDay: Int, minuteOfDay: Int): Boolean {
        if (!enabled || days.isEmpty() || startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            isoDay in days && minuteOfDay >= startMinute && minuteOfDay < endMinute
        } else {
            val previousDay = if (isoDay == 1) 7 else isoDay - 1
            (isoDay in days && minuteOfDay >= startMinute) ||
                    (previousDay in days && minuteOfDay < endMinute)
        }
    }
}

data class FocusProfile(
        val kind: FocusKind,
        val name: String,
        /** Keys from [focusAppKey]: `profileKey|packageName`. Empty means no app filtering. */
        val visibleApps: Set<String> = emptySet(),
        val layoutPreset: LayoutPreset? = null,
        val themeId: String? = null,
        val schedule: FocusSchedule = FocusSchedule(),
        val sessionMinutes: Int? = null,
        val reminder: String = "",
) {
    val id: String get() = kind.id
}

const val MAX_FOCUS_NAME = 40
const val MAX_FOCUS_REMINDER = 200
const val MAX_FOCUS_APPS = 500
const val MIN_FOCUS_SESSION_MINUTES = 1
const val MAX_FOCUS_SESSION_MINUTES = 120

object FocusDefaults {
    fun profiles(): List<FocusProfile> =
            listOf(
                    FocusProfile(FocusKind.STUDY, "Study", layoutPreset = LayoutPreset.FOCUS, sessionMinutes = 25),
                    FocusProfile(FocusKind.WORK, "Work", layoutPreset = LayoutPreset.INFORMATION, sessionMinutes = 50),
                    FocusProfile(FocusKind.PERSONAL, "Personal"),
                    FocusProfile(FocusKind.SLEEP, "Sleep", layoutPreset = LayoutPreset.PURE_MINIMAL),
                    FocusProfile(FocusKind.CUSTOM, "Custom"),
            )
}

/** Same key shape as the launcher's drawerOpenCountKey (`profile|package`). */
fun focusAppKey(packageName: String, profileKey: String): String = "$profileKey|$packageName"

/** Pure filter hook. A null/empty [allowed] set means focus filtering is off. */
fun <T> filterForFocus(items: List<T>, allowed: Set<String>?, keyOf: (T) -> String): List<T> =
        if (allowed.isNullOrEmpty()) items else items.filter { keyOf(it) in allowed }

/** Persisted activation state. */
data class FocusRuntime(
        val activeId: String? = null,
        /** True when the active profile was switched on by its schedule rather than by the user. */
        val bySchedule: Boolean = false,
        /** Scheduled profile the user switched off; ignored until its window ends. */
        val suppressedId: String? = null,
)

/** Lazy schedule evaluation, called when the app resumes. No timers or alarms are involved. */
fun resolveOnResume(
        runtime: FocusRuntime,
        profiles: List<FocusProfile>,
        isoDay: Int,
        minuteOfDay: Int,
): FocusRuntime {
    val suppressedStillInWindow =
            runtime.suppressedId?.takeIf { id ->
                profiles.firstOrNull { it.id == id }?.schedule?.isActiveAt(isoDay, minuteOfDay) == true
            }
    val match =
            profiles.firstOrNull {
                it.schedule.isActiveAt(isoDay, minuteOfDay) && it.id != suppressedStillInWindow
            }
    val base = runtime.copy(suppressedId = suppressedStillInWindow)
    return when {
        match != null && (base.activeId == null || base.bySchedule) ->
                base.copy(activeId = match.id, bySchedule = true)
        match == null && base.bySchedule -> base.copy(activeId = null, bySchedule = false)
        else -> base
    }
}
