package de.haberland.meihome.settings

import android.content.Context

data class MeiHomeDisplaySettings(
    val nightModeEnabled: Boolean = false,
    val nightStartMinutes: Int = 22 * 60,
    val nightEndMinutes: Int = 6 * 60,
    val doorbellRingtoneUri: String = "",
)

class MeiHomeSettingsPreferences(context: Context) {
    private val preferences =
        context.getSharedPreferences("meihome_display_settings", Context.MODE_PRIVATE)

    fun load(): MeiHomeDisplaySettings = MeiHomeDisplaySettings(
        nightModeEnabled = preferences.getBoolean(KEY_NIGHT_ENABLED, false),
        nightStartMinutes = preferences.getInt(KEY_NIGHT_START, 22 * 60),
        nightEndMinutes = preferences.getInt(KEY_NIGHT_END, 6 * 60),
        doorbellRingtoneUri = preferences.getString(KEY_DOORBELL_RINGTONE, "").orEmpty(),
    )

    fun save(settings: MeiHomeDisplaySettings) {
        preferences.edit()
            .putBoolean(KEY_NIGHT_ENABLED, settings.nightModeEnabled)
            .putInt(KEY_NIGHT_START, settings.nightStartMinutes)
            .putInt(KEY_NIGHT_END, settings.nightEndMinutes)
            .putString(KEY_DOORBELL_RINGTONE, settings.doorbellRingtoneUri)
            .apply()
    }

    companion object {
        private const val KEY_NIGHT_ENABLED = "night_enabled"
        private const val KEY_NIGHT_START = "night_start"
        private const val KEY_NIGHT_END = "night_end"
        private const val KEY_DOORBELL_RINGTONE = "doorbell_ringtone"
    }
}

fun MeiHomeDisplaySettings.isNightModeNow(
    hour: Int,
    minute: Int,
): Boolean {
    if (!nightModeEnabled) return false
    val now = hour * 60 + minute
    return if (nightStartMinutes == nightEndMinutes) {
        true
    } else if (nightStartMinutes < nightEndMinutes) {
        now in nightStartMinutes until nightEndMinutes
    } else {
        now >= nightStartMinutes || now < nightEndMinutes
    }
}
