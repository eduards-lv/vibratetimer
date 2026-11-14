package lv.eduards.vibratetimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    fun schedule(context: Context, timer: VibrationTimer) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, timer.id, timer.pulses)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.triggerAt, pending)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.triggerAt, pending)
        }
    }

    fun cancel(context: Context, timer: VibrationTimer) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, timer.id, timer.pulses))
    }

    private fun pendingIntent(context: Context, id: String, pulses: Int): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java)
            .putExtra(TimerReceiver.EXTRA_ID, id)
            .putExtra(TimerReceiver.EXTRA_PULSES, pulses)
        return PendingIntent.getBroadcast(context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
