package de.haberland.meihome.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.haberland.meihome.smarthome.FrontDoorConfig
import kotlinx.coroutines.launch

@Composable
fun FrontDoorConfigDialog(
    initial: FrontDoorConfig,
    onDiscoverDoorbell: suspend (String) -> Result<FrontDoorConfig>,
    onSave: (FrontDoorConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var accessToken by remember(initial) { mutableStateOf(initial.googleAccessToken) }
    var discoveredConfig by remember(initial) { mutableStateOf(initial) }
    var statusText by remember(initial) {
        mutableStateOf(
            if (initial.googleDeviceId.isBlank()) {
                "Noch keine Doorbell erkannt."
            } else {
                "Doorbell ist konfiguriert."
            },
        )
    }
    var discovering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haustür konfigurieren") },
        text = {
            Column {
                Text(
                    "Für Google Nest musst du nur noch einen gültigen Access Token eintragen. " +
                        "Project-ID und Doorbell werden automatisch ermittelt.",
                )

                OutlinedTextField(
                    value = accessToken,
                    onValueChange = {
                        accessToken = it
                        discoveredConfig = discoveredConfig.copy(
                            googleAccessToken = it.trim(),
                            googleDeviceId = "",
                        )
                        statusText = "Token geändert – Doorbell bitte neu suchen."
                    },
                    label = { Text("Google Access Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            discovering = true
                            statusText = "Suche Doorbell..."
                            onDiscoverDoorbell(accessToken.trim())
                                .onSuccess { config ->
                                    discoveredConfig = config
                                    statusText = "Google Doorbell gefunden."
                                }
                                .onFailure { error ->
                                    statusText = error.message ?: "Doorbell konnte nicht gefunden werden."
                                }
                            discovering = false
                        }
                    },
                    enabled = accessToken.isNotBlank() && !discovering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(if (discovering) "Suche..." else "Doorbell automatisch suchen")
                }

                Text(
                    text = statusText,
                    modifier = Modifier.padding(top = 10.dp),
                )

                Text(
                    text = "Der Access Token ist aktuell noch die Testlösung. " +
                        "Die automatische Token-Erneuerung kommt als nächster Schritt.",
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        discoveredConfig.copy(
                            googleAccessToken = accessToken.trim(),
                        ),
                    )
                },
                enabled = discoveredConfig.googleDeviceId.isNotBlank() &&
                    accessToken.isNotBlank() &&
                    !discovering,
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
