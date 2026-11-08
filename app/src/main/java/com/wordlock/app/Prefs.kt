package com.wordlock.app

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class LogEntry(val picture: String, val english: String, val korean: String, val time: String)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wordlock", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var level: Level
        get() = runCatching { Level.valueOf(sp.getString("level", Level.EASY.name)!!) }.getOrDefault(Level.EASY)
        set(v) = sp.edit().putString("level", v.name).apply()

    /** Emergency PIN, always 4 digits. */
    var pin: String
        get() = sp.getString("pin", "0000") ?: "0000"
        set(v) = sp.edit().putString("pin", v).apply()

    private fun rollDay() {
        val today = LocalDate.now().toString()
        if (sp.getString("day", null) != today) {
            sp.edit().putString("day", today).putInt("count", 0).putInt("streak", 0).putString("log", "").apply()
        }
    }

    val todayCount: Int get() { rollDay(); return sp.getInt("count", 0) }
    val streak: Int get() { rollDay(); return sp.getInt("streak", 0) }

    val log: List<LogEntry>
        get() {
            rollDay()
            return (sp.getString("log", "") ?: "").lines().filter { it.isNotBlank() }.mapNotNull {
                val p = it.split("\t")
                if (p.size == 4) LogEntry(p[0], p[1], p[2], p[3]) else null
            }
        }

    fun recordSolved(word: Word, usedHint: Boolean) {
        rollDay()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("H:mm"))
        val line = listOf(word.picture, word.english, word.korean, time).joinToString("\t")
        val newLog = (listOf(line) + (sp.getString("log", "") ?: "").lines().filter { it.isNotBlank() }).take(50)
        sp.edit()
            .putInt("count", sp.getInt("count", 0) + 1)
            .putInt("streak", if (usedHint) 0 else sp.getInt("streak", 0) + 1)
            .putString("log", newLog.joinToString("\n"))
            .apply()
    }

    fun resetStreak() { rollDay(); sp.edit().putInt("streak", 0).apply() }
}
