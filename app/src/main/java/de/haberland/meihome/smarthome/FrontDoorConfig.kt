package de.haberland.meihome.smarthome

/**
 * Connection details for the front-door integrations.
 *
 * Secrets are deliberately not hard-coded in the application. The UI can persist
 * these values locally once the respective Google Device Access and Nuki accounts
 * have been authorized.
 */
const val GOOGLE_DEVICE_ACCESS_PROJECT_ID = "9ae59f3c-8dbc-4551-9190-35eb9fe39db7"

data class FrontDoorConfig(
    val googleProjectId: String = GOOGLE_DEVICE_ACCESS_PROJECT_ID,
    val googleDeviceId: String = "",
    val googleAccessToken: String = "",
    val nukiDeviceId: String = "",
    val nukiAccessToken: String = "",
) {
    val googleConfigured: Boolean
        get() = googleProjectId.isNotBlank() &&
            googleDeviceId.isNotBlank() &&
            googleAccessToken.isNotBlank()

    val nukiConfigured: Boolean
        get() = nukiDeviceId.isNotBlank() && nukiAccessToken.isNotBlank()
}

enum class DoorAction {
    UNLOCK,
    LOCK,
    UNLATCH,
    LOCK_N_GO,
}

data class DoorLockState(
    val connected: Boolean,
    val label: String,
)
