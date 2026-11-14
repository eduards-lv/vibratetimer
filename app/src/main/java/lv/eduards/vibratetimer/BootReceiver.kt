package lv.eduards.vibratetimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerStore.all(context).filter { it.triggerAt > System.currentTimeMillis() }
            .forEach { AlarmScheduler.schedule(context, it) }
    }
}
