package de.haberland.meihome.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
    onSave: (FrontDoorConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var projectId by remember(initial) { mutableStateOf(initial.googleProjectId) }
    var deviceId by remember(initial) { mutableStateOf(initial.googleDeviceId) }
    var accessToken by remember(initial) { mutableStateOf(initial.googleAccessToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haustür konfigurieren") },
        text = {
            Column {
                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Google Device Access Project ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Doorbell Device Name") },
                    supportingText = { Text("Kompletter enterprises/.../devices/... Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text("Google Access Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
                Text(
                    text = "Der Access Token ist nur für den ersten Stream-Test gedacht. Automatische Token-Erneuerung folgt separat.",
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            googleProjectId = projectId.trim(),
                            googleDeviceId = deviceId.trim(),
                            googleAccessToken = accessToken.trim(),
                        ),
                    )
                },
                enabled = projectId.isNotBlank() && deviceId.isNotBlank() && accessToken.isNotBlank(),
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
