package com.fullbody.season

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** Еженедельные напоминания во вторник, четверг и субботу. */
object ReminderScheduler {

    private val days = listOf(
        Triple(Calendar.TUESDAY, 1001, "Вторник — тяжёлая тренировка. Разминка 8 минут, запас 2 повторения."),
        Triple(Calendar.THURSDAY, 1002, "Четверг — лёгкий день на одну ногу и одну руку. Запас 2–3 повторения."),
        Triple(Calendar.SATURDAY, 1003, "Суббота — тяжёлая тренировка. Разминка 8 минут, запас 2 повторения."),
    )

    fun scheduleAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ((dayOfWeek, requestCode, message) in days) {
            val pi = pendingIntent(context, requestCode, message)
            am.cancel(pi)
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTrigger(context, dayOfWeek),
                AlarmManager.INTERVAL_DAY * 7,
                pi,
            )
        }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ((_, requestCode, message) in days) {
            am.cancel(pendingIntent(context, requestCode, message))
        }
    }

    private fun nextTrigger(context: Context, dayOfWeek: Int): Long {
        val hour = com.fullbody.season.data.Prefs.reminderHour(context)
        val minute = com.fullbody.season.data.Prefs.reminderMinute(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
        return cal.timeInMillis
    }

    private fun pendingIntent(context: Context, requestCode: Int, message: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
