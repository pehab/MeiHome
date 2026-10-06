package de.haberland.meihome.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.settings.MeiHomeDisplaySettings
import de.haberland.meihome.settings.MeiHomeSettingsPreferences
import de.haberland.meihome.settings.isNightModeNow
import de.haberland.meihome.smarthome.DoorbellEventClient
import de.haberland.meihome.smarthome.DoorbellStreamStatus
import de.haberland.meihome.smarthome.DoorbellWebRtcController
import de.haberland.meihome.smarthome.FrontDoorPreferences
import de.haberland.meihome.smarthome.GOOGLE_DEVICE_ACCESS_PROJECT_ID
import de.haberland.meihome.smarthome.GoogleSdmDoorbellClient
import de.haberland.meihome.smarthome.NestOAuthManager
import de.haberland.meihome.smarthome.NestTokenManager
import de.haberland.meihome.smarthome.NukiWebClient
import de.haberland.meihome.smarthome.PubSubOAuthManager
import de.haberland.meihome.smarthome.PubSubTokenManager
import de.haberland.meihome.ui.dashboard.DashboardScreen
import de.haberland.meihome.ui.dashboard.DashboardViewModel
import de.haberland.meihome.ui.dashboard.components.FrontDoorConfigDialog
import de.haberland.meihome.ui.dashboard.components.FrontDoorDialog
import de.haberland.meihome.ui.shopping.ShoppingItemDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

private enum class ListRole {
    SHOPPING,
    TODO,
}

@Composable
fun MeiHomeApp(
    updateReadyToInstall: Boolean = false,
    onInstallUpdate: () -> Unit = {},
    nestOAuthCallback: Uri? = null,
    onNestOAuthCallbackConsumed: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val frontDoorPreferences = remember(context) { FrontDoorPreferences(context) }
    val displaySettingsPreferences = remember(context) { MeiHomeSettingsPreferences(context) }
    var displaySettings by remember { mutableStateOf(displaySettingsPreferences.load()) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val nightModeActive = displaySettings.isNightModeNow(currentTime.hour, currentTime.minute)
    val nestOAuthManager = remember(frontDoorPreferences) { NestOAuthManager(frontDoorPreferences) }
    val nestTokenManager = remember(nestOAuthManager) { NestTokenManager(nestOAuthManager) }
    val pubSubOAuthManager = remember(frontDoorPreferences) { PubSubOAuthManager(frontDoorPreferences) }
    val pubSubTokenManager = remember(pubSubOAuthManager) { PubSubTokenManager(pubSubOAuthManager) }
    val doorbellEventClient = remember(pubSubTokenManager) {
        DoorbellEventClient(pubSubTokenManager::accessToken)
    }
    var frontDoorConfig by remember { mutableStateOf(frontDoorPreferences.load()) }
    var streamStatus by remember { mutableStateOf<DoorbellStreamStatus>(DoorbellStreamStatus.Idle) }
    var nukiLockState by remember { mutableStateOf(de.haberland.meihome.smarthome.DoorLockState(false, "Nicht verbunden")) }
    var activeDoorbellRingtone by remember { mutableStateOf<Ringtone?>(null) }
    var lastDoorbellChimeAt by remember { mutableStateOf(0L) }
    var nestSetupMessage by remember { mutableStateOf<String?>(null) }
    val frontDoorScope = rememberCoroutineScope()
    val doorbellClient = remember(frontDoorConfig.googleDeviceId, nestTokenManager) {
        GoogleSdmDoorbellClient(
            configProvider = { frontDoorConfig },
            accessTokenProvider = nestTokenManager::accessToken,
        )
    }
    val nukiClient = remember(frontDoorConfig.nukiDeviceId, frontDoorPreferences) {
        NukiWebClient(
            tokenProvider = frontDoorPreferences::getNukiApiToken,
            deviceIdProvider = { frontDoorConfig.nukiDeviceId },
        )
    }
    val webRtcController = remember(doorbellClient) {
        DoorbellWebRtcController(
            context = context,
            client = doorbellClient,
            scope = frontDoorScope,
            onStatusChanged = { streamStatus = it },
        )
    }

    var updatePromptDismissed by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var frontDoorOpen by remember { mutableStateOf(false) }
    var frontDoorSettingsOpen by remember { mutableStateOf(false) }
    var selectingRole by remember { mutableStateOf<ListRole?>(null) }
    var addRole by remember { mutableStateOf<ListRole?>(null) }
    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setCalendarPermission(granted)
    }
    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uri = data?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            }
            if (uri != null) {
                displaySettings = displaySettings.copy(
                    doorbellRingtoneUri = uri.toString(),
                )
                displaySettingsPreferences.save(displaySettings)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setCalendarPermission(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(30_000L)
        }
    }

    LaunchedEffect(nightModeActive, activity) {
        activity?.window?.let { window ->
            if (nightModeActive) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = window.attributes
                params.screenBrightness = 0.01f
                window.attributes = params
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }

    LaunchedEffect(updateReadyToInstall) {
        if (!updateReadyToInstall) updatePromptDismissed = false
    }

    LaunchedEffect(nestOAuthCallback) {
        val callback = nestOAuthCallback ?: return@LaunchedEffect

        try {
            val error = callback.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                nestSetupMessage = "Google-Verbindung abgebrochen: $error"
                return@LaunchedEffect
            }

            val code = callback.getQueryParameter("code")
            if (code.isNullOrBlank()) {
                nestSetupMessage = "Google hat keinen Autorisierungscode geliefert."
                return@LaunchedEffect
            }

            val stateValue = callback.getQueryParameter("state")
            if (stateValue == PubSubOAuthManager.STATE) {
                runCatching {
                    val tokenResponse = pubSubOAuthManager.exchangeAuthorizationCode(code)
                    pubSubTokenManager.acceptInitial(tokenResponse)
                    doorbellEventClient.ensureSubscription()
                    nestSetupMessage = "Klingelereignisse sind verbunden."
                }.onFailure {
                    nestSetupMessage = it.message ?: "Klingelereignisse konnten nicht verbunden werden."
                }
            } else {
                runCatching {
                    val tokenResponse = nestOAuthManager.exchangeAuthorizationCode(code)
                    nestTokenManager.acceptInitial(tokenResponse)

                    val discovered = GoogleSdmDoorbellClient(
                        configProvider = { frontDoorConfig.copy(googleDeviceId = "") },
                        accessTokenProvider = nestTokenManager::accessToken,
                    ).listDoorbells().firstOrNull { it.supportsWebRtc }
                        ?: error("Keine WebRTC-fähige Google Doorbell gefunden.")

                    val updated = frontDoorConfig.copy(
                        googleProjectId = GOOGLE_DEVICE_ACCESS_PROJECT_ID,
                        googleDeviceId = discovered.id,
                    )
                    frontDoorPreferences.save(updated)
                    frontDoorConfig = updated
                    nestSetupMessage = "Google Doorbell verbunden."
                }.onFailure {
                    nestSetupMessage = it.message ?: "Google Home konnte nicht verbunden werden."
                }
            }
        } finally {
            onNestOAuthCallbackConsumed()
        }
    }

    LaunchedEffect(
        frontDoorConfig.googleDeviceId,
        pubSubOAuthManager.isLinked(),
        doorbellEventClient,
        nightModeActive,
        displaySettings.doorbellRingtoneUri,
    ) {
        if (frontDoorConfig.googleDeviceId.isBlank() || !pubSubOAuthManager.isLinked()) {
            return@LaunchedEffect
        }

        runCatching { doorbellEventClient.ensureSubscription() }

        while (true) {
            runCatching {
                doorbellEventClient.pullChime(frontDoorConfig.googleDeviceId)
            }.onSuccess { chime ->
                if (chime) {
                    val now = System.currentTimeMillis()
                    val isNewPress = now - lastDoorbellChimeAt >= DOORBELL_CHIME_COOLDOWN_MS
                    if (isNewPress) {
                        lastDoorbellChimeAt = now
                        if (!nightModeActive) {
                            activeDoorbellRingtone?.stop()
                            val ringtone = createDoorbellRingtone(
                                context,
                                displaySettings.doorbellRingtoneUri,
                            )
                            activeDoorbellRingtone = ringtone
                            ringtone?.play()
                            frontDoorScope.launch {
                                delay(DOORBELL_RING_DURATION_MS)
                                if (activeDoorbellRingtone === ringtone) {
                                    ringtone?.stop()
                                    activeDoorbellRingtone = null
                                }
                            }
                            frontDoorOpen = true
                        }
                    }
                }
            }
            delay(2_000L)
        }
    }

    LaunchedEffect(frontDoorOpen, frontDoorConfig.googleConfigured, webRtcController) {
        if (frontDoorOpen && frontDoorConfig.googleConfigured) {
            webRtcController.start()
        } else {
            webRtcController.stop()
        }
    }

    LaunchedEffect(frontDoorOpen, frontDoorConfig.nukiConfigured, nukiClient) {
        if (!frontDoorOpen || !frontDoorConfig.nukiConfigured) {
            nukiLockState = de.haberland.meihome.smarthome.DoorLockState(false, "Nicht verbunden")
            return@LaunchedEffect
        }

        while (true) {
            runCatching { nukiClient.getState() }
                .onSuccess { nukiLockState = it }
                .onFailure {
                    nukiLockState = de.haberland.meihome.smarthome.DoorLockState(
                        false,
                        it.message ?: "Nuki nicht erreichbar",
                    )
                }
            delay(5_000L)
        }
    }

    DisposableEffect(webRtcController) {
        onDispose {
            activeDoorbellRingtone?.stop()
            activeDoorbellRingtone = null
            webRtcController.release()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            DashboardScreen(
                state = state,
                onToggleShoppingItem = viewModel::setItemChecked,
                onToggleTodoItem = viewModel::setItemChecked,
                onSelectShoppingList = { selectingRole = ListRole.SHOPPING },
                onSelectTodoList = { selectingRole = ListRole.TODO },
                onAddShoppingItem = { addRole = ListRole.SHOPPING },
                onAddTodoItem = { addRole = ListRole.TODO },
                onRequestCalendarPermission = {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                },
                onOpenFrontDoor = { frontDoorOpen = true },
                onOpenSettings = { settingsOpen = true },
            )
        }

        if (frontDoorOpen) {
            FrontDoorDialog(
                cameraConfigured = frontDoorConfig.googleConfigured,
                streamStatus = streamStatus,
                webRtcController = webRtcController,
                nukiConnected = nukiLockState.connected,
                lockStatus = nukiLockState.label,
                onUnlock = {
                    frontDoorScope.launch {
                        runCatching { nukiClient.execute(de.haberland.meihome.smarthome.DoorAction.UNLOCK) }
                        delay(1_000L)
                        runCatching { nukiClient.getState() }.onSuccess { nukiLockState = it }
                    }
                },
                onLock = {
                    frontDoorScope.launch {
                        runCatching { nukiClient.execute(de.haberland.meihome.smarthome.DoorAction.LOCK) }
                        delay(1_000L)
                        runCatching { nukiClient.getState() }.onSuccess { nukiLockState = it }
                    }
                },
                onUnlatch = {
                    frontDoorScope.launch {
                        runCatching { nukiClient.execute(de.haberland.meihome.smarthome.DoorAction.UNLATCH) }
                        delay(1_000L)
                        runCatching { nukiClient.getState() }.onSuccess { nukiLockState = it }
                    }
                },
                onLockAndGo = {
                    frontDoorScope.launch {
                        runCatching { nukiClient.execute(de.haberland.meihome.smarthome.DoorAction.LOCK_N_GO) }
                        delay(1_000L)
                        runCatching { nukiClient.getState() }.onSuccess { nukiLockState = it }
                    }
                },
                onDismiss = {
                    activeDoorbellRingtone?.stop()
                    activeDoorbellRingtone = null
                    frontDoorOpen = false
                },
            )
        }

        if (frontDoorSettingsOpen) {
            FrontDoorConfigDialog(
                initial = frontDoorConfig,
                clientSecretConfigured = frontDoorPreferences.getNestClientSecret().isNotBlank(),
                googleLinked = nestOAuthManager.isLinked(),
                pubSubLinked = pubSubOAuthManager.isLinked(),
                nukiTokenConfigured = frontDoorPreferences.getNukiApiToken().isNotBlank(),
                onConnectGoogle = { enteredSecret ->
                    if (enteredSecret.isNotBlank()) {
                        frontDoorPreferences.setNestClientSecret(enteredSecret)
                    }
                    activity?.startActivity(
                        Intent(Intent.ACTION_VIEW, nestOAuthManager.authorizationUri()),
                    )
                },
                onConnectPubSub = {
                    activity?.startActivity(
                        Intent(Intent.ACTION_VIEW, pubSubOAuthManager.authorizationUri()),
                    )
                },
                onDiscoverDoorbell = {
                    runCatching {
                        val discoveryConfig = frontDoorConfig.copy(
                            googleProjectId = GOOGLE_DEVICE_ACCESS_PROJECT_ID,
                            googleDeviceId = "",
                        )
                        val discoveryClient = GoogleSdmDoorbellClient(
                            configProvider = { discoveryConfig },
                            accessTokenProvider = nestTokenManager::accessToken,
                        )
                        val doorbells = discoveryClient.listDoorbells().filter { it.supportsWebRtc }
                        require(doorbells.isNotEmpty()) { "Keine WebRTC-fähige Google Doorbell gefunden." }
                        discoveryConfig.copy(googleDeviceId = doorbells.first().id)
                    }
                },
                onDiscoverNuki = { enteredToken ->
                    runCatching {
                        if (enteredToken.isNotBlank()) {
                            frontDoorPreferences.setNukiApiToken(enteredToken)
                        }
                        require(frontDoorPreferences.getNukiApiToken().isNotBlank()) {
                            "Nuki API Token fehlt."
                        }
                        val locks = nukiClient.listLocks()
                        require(locks.isNotEmpty()) { "Kein Nuki Smart Lock gefunden." }
                        frontDoorConfig.copy(nukiDeviceId = locks.first().id)
                    }
                },
                onSave = { config ->
                    frontDoorPreferences.save(config)
                    frontDoorConfig = config
                    frontDoorSettingsOpen = false
                },
                onDismiss = { frontDoorSettingsOpen = false },
            )
        }

        if (settingsOpen) {
            SettingsDialog(
                isSignedIn = state.isSignedIn,
                userEmail = state.userEmail,
                shoppingListName = state.shoppingListName,
                todoListName = state.todoListName,
                calendarPermissionGranted = state.calendarPermissionGranted,
                displaySettings = displaySettings,
                doorbellRingtoneName = doorbellRingtoneName(context, displaySettings.doorbellRingtoneUri),
                onDisplaySettingsChange = { updated ->
                    displaySettings = updated
                    displaySettingsPreferences.save(updated)
                },
                onChooseDoorbellRingtone = {
                    val currentUri = displaySettings.doorbellRingtoneUri
                        .takeIf { it.isNotBlank() }
                        ?.let(Uri::parse)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtonePickerLauncher.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_RINGTONE,
                            )
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
                                true,
                            )
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                currentUri,
                            )
                        },
                    )
                },
                onSignIn = { activity?.let(viewModel::signIn) },
                onSignOut = { viewModel.signOut(context) },
                onSelectShopping = { selectingRole = ListRole.SHOPPING },
                onSelectTodo = { selectingRole = ListRole.TODO },
                onRequestCalendarPermission = {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                },
                onConfigureFrontDoor = { frontDoorSettingsOpen = true },
                onDismiss = { settingsOpen = false },
            )
        }

        selectingRole?.let { role ->
            ListSelectionDialog(
                title = if (role == ListRole.SHOPPING) "Einkaufsliste auswählen" else "Todo-Liste auswählen",
                lists = state.availableLists,
                selectedId = if (role == ListRole.SHOPPING) state.shoppingListId else state.todoListId,
                onSelect = { id ->
                    if (role == ListRole.SHOPPING) viewModel.selectShoppingList(id) else viewModel.selectTodoList(id)
                    selectingRole = null
                },
                onDismiss = { selectingRole = null },
            )
        }

        addRole?.let { role ->
            if (role == ListRole.SHOPPING) {
                val list = state.availableLists.firstOrNull { it.id == state.shoppingListId }
                if (state.isSignedIn && list != null) {
                    ShoppingItemDialog(list = list, onDismiss = { addRole = null })
                } else {
                    LaunchedEffect(Unit) { addRole = null }
                }
            } else {
                AddItemDialog(
                    title = "Todo hinzufügen",
                    onAdd = { text ->
                        viewModel.addTodoItem(text)
                        addRole = null
                    },
                    onDismiss = { addRole = null },
                )
            }
        }

        nestSetupMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { nestSetupMessage = null },
                title = { Text("Google Home") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { nestSetupMessage = null }) {
                        Text("OK")
                    }
                },
            )
        }

        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                title = { Text("Fehler") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK")
                    }
                },
            )
        }

        if (updateReadyToInstall && !updatePromptDismissed) {
            AlertDialog(
                onDismissRequest = { updatePromptDismissed = true },
                title = { Text("Update bereit") },
                text = { Text("Eine neue Version von MeiHome wurde heruntergeladen und kann jetzt installiert werden.") },
                confirmButton = {
                    TextButton(onClick = onInstallUpdate) {
                        Text("Installieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updatePromptDismissed = true }) {
                        Text("Später")
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    isSignedIn: Boolean,
    userEmail: String?,
    shoppingListName: String?,
    todoListName: String?,
    calendarPermissionGranted: Boolean,
    displaySettings: MeiHomeDisplaySettings,
    doorbellRingtoneName: String,
    onDisplaySettingsChange: (MeiHomeDisplaySettings) -> Unit,
    onChooseDoorbellRingtone: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSelectShopping: () -> Unit,
    onSelectTodo: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onConfigureFrontDoor: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MeiHome Einstellungen") },
        text = {
            Column {
                Text(
                    if (isSignedIn) {
                        "Angemeldet als " + (userEmail ?: "Google-Konto")
                    } else {
                        "Nicht angemeldet"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (isSignedIn) {
                    TextButton(onClick = onSelectShopping) {
                        Text("Einkauf: " + (shoppingListName ?: "Liste auswählen"))
                    }
                    TextButton(onClick = onSelectTodo) {
                        Text("Todos: " + (todoListName ?: "Liste auswählen"))
                    }
                    TextButton(onClick = onRequestCalendarPermission) {
                        Text(
                            if (calendarPermissionGranted) {
                                "Kalenderzugriff: erlaubt"
                            } else {
                                "Kalenderzugriff erlauben"
                            },
                        )
                    }
                    TextButton(onClick = onConfigureFrontDoor) {
                        Text("Haustür konfigurieren")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Nachtmodus",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = displaySettings.nightModeEnabled,
                            onCheckedChange = {
                                onDisplaySettingsChange(
                                    displaySettings.copy(nightModeEnabled = it),
                                )
                            },
                        )
                    }

                    if (displaySettings.nightModeEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Von", modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    showTimePicker(
                                        context = context,
                                        minutes = displaySettings.nightStartMinutes,
                                    ) { minutes ->
                                        onDisplaySettingsChange(
                                            displaySettings.copy(nightStartMinutes = minutes),
                                        )
                                    }
                                },
                            ) {
                                Text(formatMinutes(displaySettings.nightStartMinutes))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Bis", modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    showTimePicker(
                                        context = context,
                                        minutes = displaySettings.nightEndMinutes,
                                    ) { minutes ->
                                        onDisplaySettingsChange(
                                            displaySettings.copy(nightEndMinutes = minutes),
                                        )
                                    }
                                },
                            ) {
                                Text(formatMinutes(displaySettings.nightEndMinutes))
                            }
                        }
                    }

                    TextButton(
                        onClick = onChooseDoorbellRingtone,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Klingelton: $doorbellRingtoneName")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(onClick = onSignOut) {
                        Text("Abmelden")
                    }
                } else {
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Mit Google anmelden")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
    )
}

@Composable
private fun ListSelectionDialog(
    title: String,
    lists: List<MeiList>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (lists.isEmpty()) {
                Text("Keine freigegebenen MeiLists-Listen gefunden.")
            } else {
                LazyColumn {
                    items(lists, key = { it.id }) { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(list.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = list.id == selectedId,
                                onClick = { onSelect(list.id) },
                            )
                            Text(list.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
private fun AddItemDialog(
    title: String,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Eintrag") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}


private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private fun showTimePicker(
    context: Context,
    minutes: Int,
    onSelected: (Int) -> Unit,
) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(hour * 60 + minute) },
        minutes / 60,
        minutes % 60,
        true,
    ).show()
}

private fun doorbellRingtoneName(
    context: Context,
    uriValue: String,
): String {
    val uri = uriValue
        .takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    return runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull().orEmpty().ifBlank { "Standard" }
}

private fun createDoorbellRingtone(
    context: Context,
    uriValue: String,
): Ringtone? {
    val uri = uriValue
        .takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    return runCatching {
        RingtoneManager.getRingtone(context, uri)
    }.getOrNull()
}

private const val DOORBELL_RING_DURATION_MS = 5_000L
private const val DOORBELL_CHIME_COOLDOWN_MS = 15_000L

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
