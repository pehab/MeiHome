package de.haberland.meihome.smarthome

import android.content.Context

class FrontDoorPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("front_door", Context.MODE_PRIVATE)
    private val secrets = SecureSecretStore(context)

    fun load(): FrontDoorConfig = FrontDoorConfig(
        googleProjectId = preferences
            .getString(KEY_GOOGLE_PROJECT_ID, GOOGLE_DEVICE_ACCESS_PROJECT_ID)
            .orEmpty()
            .ifBlank { GOOGLE_DEVICE_ACCESS_PROJECT_ID },
        googleDeviceId = preferences.getString(KEY_GOOGLE_DEVICE_ID, "").orEmpty(),
        nukiDeviceId = preferences.getString(KEY_NUKI_DEVICE_ID, "").orEmpty(),
    )

    fun save(config: FrontDoorConfig) {
        preferences.edit()
            .putString(KEY_GOOGLE_PROJECT_ID, config.googleProjectId.trim())
            .putString(KEY_GOOGLE_DEVICE_ID, config.googleDeviceId.trim())
            .putString(KEY_NUKI_DEVICE_ID, config.nukiDeviceId.trim())
            .apply()
    }

    fun getNestClientSecret(): String = secrets.get(SecureSecretStore.NEST_CLIENT_SECRET)
    fun setNestClientSecret(value: String) = secrets.put(SecureSecretStore.NEST_CLIENT_SECRET, value)
    fun getNestRefreshToken(): String = secrets.get(SecureSecretStore.NEST_REFRESH_TOKEN)
    fun setNestRefreshToken(value: String) = secrets.put(SecureSecretStore.NEST_REFRESH_TOKEN, value)
    fun getPubSubRefreshToken(): String = secrets.get(SecureSecretStore.PUBSUB_REFRESH_TOKEN)
    fun setPubSubRefreshToken(value: String) = secrets.put(SecureSecretStore.PUBSUB_REFRESH_TOKEN, value)
    fun getNukiApiToken(): String = secrets.get(SecureSecretStore.NUKI_API_TOKEN)
    fun setNukiApiToken(value: String) = secrets.put(SecureSecretStore.NUKI_API_TOKEN, value)

    companion object {
        private const val KEY_GOOGLE_PROJECT_ID = "google_project_id"
        private const val KEY_GOOGLE_DEVICE_ID = "google_device_id"
        private const val KEY_NUKI_DEVICE_ID = "nuki_device_id"
    }
}
