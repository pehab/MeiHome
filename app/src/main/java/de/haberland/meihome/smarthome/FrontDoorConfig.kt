package de.haberland.meihome.smarthome

/**
 * Connection details for the front-door integrations.
 *
 * Secrets are deliberately not hard-coded in the application. The UI can persist
 * these values locally once the respective Google Device Access and Nuki accounts
 * have been authorized.
 */
data class FrontDoorConfig(
    val googleProjectId: String = "",
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
