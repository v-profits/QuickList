package com.quicklist.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.quicklist.app.data.ItemEntity

object ReminderScheduler {
    const val ACTION_REMIND = "com.quicklist.app.REMIND"
    const val ACTION_DONE = "com.quicklist.app.DONE"
    const val EXTRA_ID = "item_id"

    fun schedule(context: Context, item: ItemEntity) {
        val due = item.dueAt
        if (item.done || due == null || due <= System.currentTimeMillis()) {
            cancel(context, item.id)
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact alarm: no special permission needed, fires within a few minutes of the time.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pendingIntent(context, item.id))
    }

    fun cancel(context: Context, itemId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, itemId))
    }

    private fun pendingIntent(context: Context, itemId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_ID, itemId)
        return PendingIntent.getBroadcast(
            context, itemId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
