package de.haberland.meihome.smarthome

const val GOOGLE_DEVICE_ACCESS_PROJECT_ID = "9ae59f3c-8dbc-4551-9190-35eb9fe39db7"
const val GOOGLE_NEST_OAUTH_CLIENT_ID =
    "360704856415-5l8u5an59m09duis3bl4cmphrl37o08o.apps.googleusercontent.com"
const val GOOGLE_NEST_REDIRECT_URI =
    "https://pehab.github.io/MeiHome/oauth-callback.html"
const val GOOGLE_NEST_CALLBACK_SCHEME = "meihome"
const val GOOGLE_NEST_CALLBACK_HOST = "nest-auth"

data class FrontDoorConfig(
    val googleProjectId: String = GOOGLE_DEVICE_ACCESS_PROJECT_ID,
    val googleDeviceId: String = "",
    val nukiDeviceId: String = "",
) {
    val googleConfigured: Boolean
        get() = googleDeviceId.isNotBlank()

    val nukiConfigured: Boolean
        get() = nukiDeviceId.isNotBlank()
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
