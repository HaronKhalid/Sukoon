package com.lu4p.fokuslauncher.focus

import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.MiniJson

/**
 * Versioned JSON for edited profiles. Any malformed, oversized or future-version input yields the
 * defaults; individual invalid profiles fall back to their default. Unknown keys are ignored.
 */
object FocusCodec {
    const val SCHEMA_VERSION = 1

    fun encode(profiles: List<FocusProfile>): String {
        val items =
                profiles.joinToString(",") { p ->
                    buildString {
                        append("{\"id\":").append(MiniJson.quote(p.id))
                        append(",\"name\":").append(MiniJson.quote(p.name))
                        append(",\"apps\":[")
                        append(p.visibleApps.sorted().joinToString(",") { MiniJson.quote(it) })
                        append("]")
                        p.layoutPreset?.let { append(",\"layout\":").append(MiniJson.quote(it.name)) }
                        p.themeId?.let { append(",\"theme\":").append(MiniJson.quote(it)) }
                        append(",\"schedule\":{\"enabled\":").append(p.schedule.enabled)
                        append(",\"days\":[").append(p.schedule.days.sorted().joinToString(",")).append("]")
                        append(",\"start\":").append(p.schedule.startMinute)
                        append(",\"end\":").append(p.schedule.endMinute).append("}")
                        p.sessionMinutes?.let { append(",\"session\":").append(it) }
                        append(",\"reminder\":").append(MiniJson.quote(p.reminder))
                        append("}")
                    }
                }
        return "{\"schemaVersion\":$SCHEMA_VERSION,\"profiles\":[$items]}"
    }

    /** Always returns all five profiles, defaults overlaid with whatever validates. */
    fun decodeOrDefault(text: String?): List<FocusProfile> {
        val defaults = FocusDefaults.profiles()
        if (text.isNullOrBlank()) return defaults
        val root =
                try {
                    MiniJson.parse(text) as? Map<*, *>
                } catch (e: MiniJson.JsonException) {
                    null
                } ?: return defaults
        if ((root["schemaVersion"] as? Number)?.toInt() != SCHEMA_VERSION) return defaults
        val list = root["profiles"] as? List<*> ?: return defaults
        val parsed = list.mapNotNull { (it as? Map<*, *>)?.let(::parseProfile) }.associateBy { it.kind }
        return defaults.map { parsed[it.kind] ?: it }
    }

    private fun parseProfile(m: Map<*, *>): FocusProfile? {
        val kind = FocusKind.fromId(m["id"] as? String ?: return null) ?: return null
        val name = (m["name"] as? String)?.trim() ?: return null
        if (name.isEmpty() || name.length > MAX_FOCUS_NAME) return null
        val apps = (m["apps"] as? List<*>) ?: return null
        if (apps.size > MAX_FOCUS_APPS || apps.any { it !is String || !validAppKey(it) }) return null
        val layoutName = m["layout"]
        val layout =
                if (layoutName == null) null
                else LayoutPreset.entries.firstOrNull { it.name == layoutName } ?: return null
        val themeRaw = m["theme"]
        val theme =
                if (themeRaw == null) null
                else (themeRaw as? String)?.takeIf { it.isNotBlank() && it.length <= 64 } ?: return null
        val sm = m["schedule"] as? Map<*, *> ?: return null
        val days = (sm["days"] as? List<*>) ?: return null
        val dayInts = days.map { (it as? Number)?.toInt() ?: return null }
        if (dayInts.any { it !in 1..7 }) return null
        val start = (sm["start"] as? Number)?.toInt() ?: return null
        val end = (sm["end"] as? Number)?.toInt() ?: return null
        if (start !in 0..1439 || end !in 0..1439) return null
        val enabled = sm["enabled"] as? Boolean ?: return null
        val sessionRaw = m["session"]
        val session =
                if (sessionRaw == null) null
                else
                        (sessionRaw as? Number)?.toInt()?.takeIf {
                            it in MIN_FOCUS_SESSION_MINUTES..MAX_FOCUS_SESSION_MINUTES
                        } ?: return null
        val reminder = m["reminder"] as? String ?: ""
        if (reminder.length > MAX_FOCUS_REMINDER) return null
        return FocusProfile(
                kind = kind,
                name = name,
                visibleApps = apps.map { it as String }.toSet(),
                layoutPreset = layout,
                themeId = theme,
                schedule = FocusSchedule(enabled, dayInts.toSet(), start, end),
                sessionMinutes = session,
                reminder = reminder,
        )
    }

    private fun validAppKey(s: String) = s.length in 3..256 && s.contains('|')
}
