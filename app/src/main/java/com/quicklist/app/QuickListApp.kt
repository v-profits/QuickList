package com.quicklist.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.quicklist.app.data.Prefs
import com.quicklist.app.reminders.ReminderReceiver

class QuickListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(Prefs(this).theme))
        ReminderReceiver.createChannel(this)
    }
}
