package com.quicklist.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quicklist.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Repository.get(context).rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
