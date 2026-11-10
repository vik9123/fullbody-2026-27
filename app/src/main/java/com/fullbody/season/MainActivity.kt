package com.fullbody.season

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fullbody.season.data.Prefs
import com.fullbody.season.data.Program
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val dateFmt = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        findViewById<Button>(R.id.dayTue).setOnClickListener { openDay("TUE") }
        findViewById<Button>(R.id.dayThu).setOnClickListener { openDay("THU") }
        findViewById<Button>(R.id.daySat).setOnClickListener { openDay("SAT") }
        findViewById<Button>(R.id.progressButton).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderWeek()
        renderDays()
    }

    private fun renderWeek() {
        val absolute = Prefs.absoluteWeek(this)
        val cycle = Prefs.cycleWeek(this)
        findViewById<TextView>(R.id.weekTitle).text = "Неделя $absolute · неделя $cycle из 8 в цикле"
        val mode = when (cycle) {
            1 -> "Калибровочная неделя: где 4 подхода — делайте 3. Задача — записать рабочие веса."
            8 -> "Разгрузочная неделя: подходов вдвое меньше, веса те же."
            else -> "Обычная неделя. Тяжёлые дни: запас 2 повторения. Четверг: запас 2–3, растут повторения."
        }
        findViewById<TextView>(R.id.weekMode).text = mode
    }

    private fun renderDays() {
        val today = LocalDate.now()
        for ((buttonId, plan, dow) in listOf(
            Triple(R.id.dayTue, Program.dayById("TUE"), DayOfWeek.TUESDAY),
            Triple(R.id.dayThu, Program.dayById("THU"), DayOfWeek.THURSDAY),
            Triple(R.id.daySat, Program.dayById("SAT"), DayOfWeek.SATURDAY),
        )) {
            val shift = (dow.value - today.dayOfWeek.value + 7) % 7
            val next = today.plusDays(shift.toLong())
            val whenText = if (shift == 0) "сегодня" else next.format(dateFmt)
            findViewById<Button>(buttonId).text =
                "${plan.title.uppercase(Locale("ru"))} · $whenText\n${plan.subtitle}"
        }
    }

    private fun openDay(id: String) {
        startActivity(Intent(this, WorkoutActivity::class.java).putExtra(WorkoutActivity.EXTRA_DAY, id))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
