package lv.eduards.vibratetimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class VibrationTimer(val id: String, val triggerAt: Long, val pulses: Int)

object TimerStore {
    private const val PREFS = "vibration_timers"
    private const val KEY = "items"

    fun all(context: Context): List<VibrationTimer> {
        val data = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(data)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                VibrationTimer(o.getString("id"), o.getLong("at"), o.getInt("pulses"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, timers: List<VibrationTimer>) {
        val array = JSONArray()
        timers.forEach { timer ->
            array.put(JSONObject().put("id", timer.id).put("at", timer.triggerAt).put("pulses", timer.pulses))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun create(triggerAt: Long, pulses: Int) = VibrationTimer(UUID.randomUUID().toString(), triggerAt, pulses)
}
