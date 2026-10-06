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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.haberland.meihome.smarthome.FrontDoorConfig

@Composable
fun FrontDoorConfigDialog(
    initial: FrontDoorConfig,
    clientSecretConfigured: Boolean,
    googleLinked: Boolean,
    pubSubLinked: Boolean,
    nukiTokenConfigured: Boolean,
    onConnectGoogle: (String) -> Unit,
    onConnectPubSub: () -> Unit,
    onDiscoverDoorbell: suspend () -> Result<FrontDoorConfig>,
    onDiscoverNuki: suspend (String) -> Result<FrontDoorConfig>,
    onSave: (FrontDoorConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var clientSecret by remember { mutableStateOf("") }
    var nukiToken by remember { mutableStateOf("") }
    var discoveredConfig by remember(initial) { mutableStateOf(initial) }
    var statusText by remember(initial, googleLinked) {
        mutableStateOf(
            when {
                initial.googleDeviceId.isNotBlank() -> "Google Doorbell ist konfiguriert."
                googleLinked -> "Google Home ist verbunden. Doorbell kann gesucht werden."
                else -> "Google Home ist noch nicht verbunden."
            },
        )
    }
    var discovering by remember { mutableStateOf(false) }
    var discoveringNuki by remember { mutableStateOf(false) }
    var nukiStatusText by remember(initial) {
        mutableStateOf(
            if (initial.nukiDeviceId.isNotBlank()) {
                "Nuki Smart Lock ist konfiguriert."
            } else {
                "Nuki ist noch nicht verbunden."
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haustür konfigurieren") },
        text = {
            Column {
                Text(
                    "Das Client Secret wird nur lokal auf diesem Tablet gespeichert und mit dem Android Keystore verschlüsselt.",
                )

                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = {
                        Text(
                            if (clientSecretConfigured) {
                                "Google Client Secret (bereits gespeichert)"
                            } else {
                                "Google Client Secret"
                            },
                        )
                    },
                    placeholder = {
                        if (clientSecretConfigured) Text("Leer lassen, um das gespeicherte Secret zu behalten")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                Button(
                    onClick = { onConnectGoogle(clientSecret.trim()) },
                    enabled = clientSecretConfigured || clientSecret.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(if (googleLinked) "Google Home neu verbinden" else "Google Home verbinden")
                }

                OutlinedButton(
                    onClick = onConnectPubSub,
                    enabled = googleLinked && !pubSubLinked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(if (pubSubLinked) "Klingelereignisse verbunden" else "Klingelereignisse verbinden")
                }

                OutlinedButton(
                    onClick = {
                        discovering = true
                        statusText = "Suche Doorbell..."
                    },
                    enabled = googleLinked && !discovering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(if (discovering) "Suche..." else "Doorbell automatisch suchen")
                }

                if (discovering) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        onDiscoverDoorbell()
                            .onSuccess {
                                discoveredConfig = it
                                statusText = "Google Doorbell gefunden."
                            }
                            .onFailure {
                                statusText = it.message ?: "Doorbell konnte nicht gefunden werden."
                            }
                        discovering = false
                    }
                }

                Text(statusText, modifier = Modifier.padding(top = 10.dp))
                if (pubSubLinked) {
                    Text(
                        "Klingelereignisse sind aktiv. Beim Klingeln öffnet MeiHome automatisch die Haustür.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                )

                Text("Nuki", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = nukiToken,
                    onValueChange = { nukiToken = it },
                    label = {
                        Text(
                            if (nukiTokenConfigured) {
                                "Nuki API Token (bereits gespeichert)"
                            } else {
                                "Nuki API Token"
                            },
                        )
                    },
                    placeholder = {
                        if (nukiTokenConfigured) {
                            Text("Leer lassen, um den gespeicherten Token zu behalten")
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                OutlinedButton(
                    onClick = { discoveringNuki = true },
                    enabled = (nukiTokenConfigured || nukiToken.isNotBlank()) && !discoveringNuki,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(if (discoveringNuki) "Suche..." else "Nuki automatisch suchen")
                }

                if (discoveringNuki) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        onDiscoverNuki(nukiToken.trim())
                            .onSuccess {
                                discoveredConfig = it
                                nukiStatusText = "Nuki Smart Lock gefunden."
                            }
                            .onFailure {
                                nukiStatusText = it.message ?: "Nuki konnte nicht gefunden werden."
                            }
                        discoveringNuki = false
                    }
                }

                Text(nukiStatusText, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(discoveredConfig) },
                enabled = discoveredConfig.googleDeviceId.isNotBlank() &&
                    !discovering &&
                    !discoveringNuki,
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
    )
}
