package com.fullbody.season.data

import android.content.Context
import java.time.LocalDate

object Prefs {
    private const val FILE = "settings"
    private const val KEY_START_DATE = "start_date"
    private const val KEY_REMINDERS = "reminders_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun startDate(context: Context): LocalDate {
        val stored = prefs(context).getString(KEY_START_DATE, null)
        return stored?.let { LocalDate.parse(it) } ?: LocalDate.now().also { setStartDate(context, it) }
    }

    fun setStartDate(context: Context, date: LocalDate) {
        prefs(context).edit().putString(KEY_START_DATE, date.toString()).apply()
    }

    /** Номер недели внутри 8-недельного цикла: 1..8. */
    fun cycleWeek(context: Context, on: LocalDate = LocalDate.now()): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate(context), on)
        val absoluteWeek = (days / 7).toInt() + 1
        return ((absoluteWeek - 1) % 8) + 1
    }

    fun absoluteWeek(context: Context, on: LocalDate = LocalDate.now()): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate(context), on)
        return (days / 7).toInt() + 1
    }

    fun remindersEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDERS, false)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDERS, enabled).apply()
    }

    fun reminderHour(context: Context): Int = prefs(context).getInt(KEY_HOUR, 18)
    fun reminderMinute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, 0)

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }
}
