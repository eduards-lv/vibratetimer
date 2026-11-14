package lv.eduards.vibratetimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.Notification
import android.app.PendingIntent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val pulses = intent.getIntExtra(EXTRA_PULSES, 1).coerceIn(1, 30)
        val pendingResult = goAsync()
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val timers = TimerStore.all(context)
                TimerStore.save(context, timers.filterNot { it.id == id })
                vibrate(context, pulses)
                showNotification(context, pulses)
            } finally {
                pendingResult.finish()
                executor.shutdown()
            }
        }
    }

    private fun vibrate(context: Context, pulses: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = LongArray(pulses * 2 + 1) { index -> if (index % 2 == 0) 180L else 220L }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun showNotification(context: Context, pulses: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Таймеры", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Время вышло")
            .setContentText("Вибраций: $pulses")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val EXTRA_ID = "timer_id"
        const val EXTRA_PULSES = "timer_pulses"
        const val CHANNEL = "timer_alerts"
    }
}
