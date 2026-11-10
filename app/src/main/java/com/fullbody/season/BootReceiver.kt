package com.fullbody.season

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fullbody.season.data.Prefs

/** Восстанавливает напоминания после перезагрузки телефона. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.remindersEnabled(context)) {
            ReminderScheduler.scheduleAll(context)
        }
    }
}
