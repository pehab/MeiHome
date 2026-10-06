package de.haberland.meihome.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haberland.meihome.smarthome.FrontDoorConfig
import kotlinx.coroutines.launch

@Composable
fun FrontDoorConfigDialog(
    initial: FrontDoorConfig,
    onDiscoverDoorbell: suspend () -> Result<FrontDoorConfig>,
    onSave: (FrontDoorConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var discoveredConfig by remember(initial) { mutableStateOf(initial) }
    var statusText by remember(initial) {
        mutableStateOf(if (initial.googleDeviceId.isBlank()) "Noch keine Doorbell erkannt." else "Doorbell ist konfiguriert.")
    }
    var discovering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haustür konfigurieren") },
        text = {
            Column {
                Text("Google-Zugriff wird automatisch über MeiHome erneuert. Du musst keinen Access Token mehr eintragen.")
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            discovering = true
                            statusText = "Suche Doorbell..."
                            onDiscoverDoorbell()
                                .onSuccess {
                                    discoveredConfig = it
                                    statusText = "Google Doorbell gefunden."
                                }
                                .onFailure { statusText = it.message ?: "Doorbell konnte nicht gefunden werden." }
                            discovering = false
                        }
                    },
                    enabled = !discovering,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(if (discovering) "Suche..." else "Doorbell automatisch suchen")
                }
                Text(statusText, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(discoveredConfig) },
                enabled = discoveredConfig.googleDeviceId.isNotBlank() && !discovering,
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
