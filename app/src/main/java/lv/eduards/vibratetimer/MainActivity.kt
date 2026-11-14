package lv.eduards.vibratetimer

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private val dateFormat = SimpleDateFormat("HH:mm:ss · dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotifications()
        buildUi()
        refresh()
        requestExactAlarmAccess()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(18))
            setBackgroundColor(0xFFF7F7FB.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Vibrate Timer"
            textSize = 30f
            setTextColor(0xFF17172B.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Тихий сигнал в нужное время"
            textSize = 15f
            setTextColor(0xFF77778A.toInt())
            setPadding(0, dp(5), 0, dp(20))
        })
        root.addView(Button(this).apply {
            text = "+  Добавить таймер"
            textSize = 16f
            isAllCaps = false
            setOnClickListener { addTimer() }
        })
        empty = TextView(this).apply {
            text = "Пока нет активных таймеров"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF77778A.toInt())
            setPadding(0, dp(35), 0, dp(20))
        }
        root.addView(empty)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addTimer() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        val minutesInput = EditText(this).apply {
            hint = "Через сколько минут?"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("5")
        }
        val pulseInput = EditText(this).apply {
            hint = "Количество вибраций"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("2")
        }
        layout.addView(minutesInput)
        layout.addView(pulseInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Новый таймер")
            .setView(layout)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Запустить", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val minutes = minutesInput.text.toString().toLongOrNull()
                val pulses = pulseInput.text.toString().toIntOrNull()
                if (minutes == null || minutes < 1 || pulses == null || pulses !in 1..30) {
                    if (minutes == null || minutes < 1) minutesInput.error = "Укажите минуты (от 1)"
                    if (pulses == null || pulses !in 1..30) pulseInput.error = "От 1 до 30 вибраций"
                    return@setOnClickListener
                }
                val timer = TimerStore.create(System.currentTimeMillis() + minutes * 60_000L, pulses)
                TimerStore.save(this, TimerStore.all(this) + timer)
                AlarmScheduler.schedule(this, timer)
                refresh()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun refresh() {
        list.removeAllViews()
        val original = TimerStore.all(this)
        val timers = original.filter { it.triggerAt > System.currentTimeMillis() }.sortedBy { it.triggerAt }
        if (timers.size != original.size) TimerStore.save(this, timers)
        empty.visibility = if (timers.isEmpty()) View.VISIBLE else View.GONE
        timers.forEach { timer ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(10))
                setBackgroundColor(0xFFFFFFFF.toInt())
                val params = LinearLayout.LayoutParams(-1, -2)
                params.bottomMargin = dp(12)
                layoutParams = params
            }
            card.addView(TextView(this).apply {
                text = dateFormat.format(Date(timer.triggerAt))
                textSize = 20f
                setTextColor(0xFF20203A.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            card.addView(TextView(this).apply {
                text = "${timer.pulses} ${plural(timer.pulses, "вибрация", "вибрации", "вибраций")}"
                textSize = 15f
                setTextColor(0xFF77778A.toInt())
                setPadding(0, dp(4), 0, 0)
            })
            card.addView(Button(this).apply {
                text = "Отменить таймер"
                isAllCaps = false
                setOnClickListener {
                    AlarmScheduler.cancel(this@MainActivity, timer)
                    TimerStore.save(this@MainActivity, TimerStore.all(this@MainActivity).filterNot { it.id == timer.id })
                    refresh()
                }
            })
            list.addView(card)
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarms = getSystemService(AlarmManager::class.java)
            if (!alarms.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Разрешить точные таймеры")
                    .setMessage("Чтобы таймер сработал вовремя при заблокированном экране, разрешите приложению устанавливать точные будильники.")
                    .setNegativeButton("Позже", null)
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                    }.show()
            }
        }
    }

    private fun plural(n: Int, one: String, few: String, many: String) =
        if (n % 10 == 1 && n % 100 != 11) one
        else if (n % 10 in 2..4 && n % 100 !in 12..14) few else many

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
