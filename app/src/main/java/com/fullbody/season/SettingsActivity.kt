package com.fullbody.season

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fullbody.season.data.Prefs
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.LocalDate
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var remindersSwitch: MaterialSwitch
    private lateinit var timeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        remindersSwitch = findViewById(R.id.remindersSwitch)
        timeButton = findViewById(R.id.timeButton)

        remindersSwitch.isChecked = Prefs.remindersEnabled(this)
        renderTime()

        timeButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    Prefs.setReminderTime(this, hour, minute)
                    renderTime()
                    if (remindersSwitch.isChecked) ReminderScheduler.scheduleAll(this)
                },
                Prefs.reminderHour(this),
                Prefs.reminderMinute(this),
                true,
            ).show()
        }

        remindersSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setRemindersEnabled(this, checked)
            if (checked) {
                ReminderScheduler.scheduleAll(this)
                Toast.makeText(this, "Напоминания включены: Вт, Чт, Сб", Toast.LENGTH_SHORT).show()
            } else {
                ReminderScheduler.cancelAll(this)
                Toast.makeText(this, "Напоминания выключены", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.resetCycleButton).setOnClickListener {
            Prefs.setStartDate(this, LocalDate.now())
            Toast.makeText(this, "Цикл начат заново: сегодня — неделя 1 (калибровочная)", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderTime() {
        timeButton.text = String.format(
            Locale.US, "Время напоминания: %02d:%02d",
            Prefs.reminderHour(this), Prefs.reminderMinute(this),
        )
    }
}
