package com.lu4p.fokuslauncher.reminders

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned JSON: `{"v":1,"items":[...]}`. Decoding is forgiving (bad entries are skipped, never
 * throws); the list is capped at [MAX_REMINDERS] on both encode and decode.
 */
object ReminderCodec {
    const val VERSION = 1

    fun encode(list: List<Reminder>): String {
        val arr = JSONArray()
        ReminderMath.cap(list).forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("t", r.title); put("n", r.note)
                put("due", r.dueEpochMs); put("z", r.zoneId); put("r", r.repeat.name)
                put("d", r.done); put("nf", r.notify); put("c", r.colorArgb); put("s", r.snoozeUntilMs)
            })
        }
        return JSONObject().put("v", VERSION).put("items", arr).toString()
    }

    fun decode(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank() || raw.length > 2_000_000) return emptyList()
        return try {
            val arr = JSONObject(raw).optJSONArray("items") ?: return emptyList()
            val out = ArrayList<Reminder>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank() || !o.has("due")) continue
                out += Reminder(
                        id = id,
                        title = o.optString("t"),
                        note = o.optString("n"),
                        dueEpochMs = o.optLong("due"),
                        zoneId = o.optString("z").ifBlank { java.time.ZoneId.systemDefault().id },
                        repeat = runCatching { RepeatRule.valueOf(o.optString("r")) }.getOrDefault(RepeatRule.NONE),
                        done = o.optBoolean("d"),
                        notify = o.optBoolean("nf", true),
                        colorArgb = o.optInt("c"),
                        snoozeUntilMs = o.optLong("s"),
                )
            }
            ReminderMath.cap(out.distinctBy { it.id })
        } catch (_: Exception) {
            emptyList()
        }
    }
}
