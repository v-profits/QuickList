package com.quicklist.app.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 0 = system, 1 = light, 2 = dark */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit { putInt("theme", v) }

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamic", true)
        set(v) = sp.edit { putBoolean("dynamic", v) }

    var autoKeyboard: Boolean
        get() = sp.getBoolean("auto_keyboard", true)
        set(v) = sp.edit { putBoolean("auto_keyboard", v) }

    var muteVoiceBeep: Boolean
        get() = sp.getBoolean("mute_beep", true)
        set(v) = sp.edit { putBoolean("mute_beep", v) }

    var lastListId: Long
        get() = sp.getLong("last_list", -1L)
        set(v) = sp.edit { putLong("last_list", v) }

    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) = sp.edit { putBoolean("seeded", v) }

    companion object {
        fun nightMode(theme: Int): Int = when (theme) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
