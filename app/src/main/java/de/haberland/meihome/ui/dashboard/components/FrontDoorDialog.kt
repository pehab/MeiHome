package de.haberland.meihome.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FrontDoorDialog(
    cameraConnected: Boolean = false,
    nukiConnected: Boolean = false,
    lockStatus: String = "Nicht verbunden",
    onUnlock: () -> Unit = {},
    onLock: () -> Unit = {},
    onUnlatch: () -> Unit = {},
    onLockAndGo: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 16.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DoorFront,
                        contentDescription = null,
                    )
                    Text(
                        text = "Haustür",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CameraPanel(
                        connected = cameraConnected,
                        modifier = Modifier
                            .weight(1.7f)
                            .fillMaxHeight(),
                    )
                    DoorControls(
                        connected = nukiConnected,
                        lockStatus = lockStatus,
                        onUnlock = onUnlock,
                        onLock = onLock,
                        onUnlatch = onUnlatch,
                        onLockAndGo = onLockAndGo,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPanel(
    connected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!connected) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Text(
                        text = "Kamerastream",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Noch nicht verbunden",
                        color = Color.LightGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun DoorControls(
    connected: Boolean,
    lockStatus: String,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onUnlatch: () -> Unit,
    onLockAndGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Türsteuerung", style = MaterialTheme.typography.titleLarge)

        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = lockStatus,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Button(
            onClick = onUnlock,
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null)
            Text("Aufsperren", modifier = Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = onLock,
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Text("Zusperren", modifier = Modifier.padding(start = 8.dp))
        }

        OutlinedButton(
            onClick = onUnlatch,
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.DoorFront, contentDescription = null)
            Text("Tür öffnen", modifier = Modifier.padding(start = 8.dp))
        }

        OutlinedButton(
            onClick = onLockAndGo,
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Lock ’n’ Go")
        }

        if (!connected) {
            Text(
                text = "Nuki ist noch nicht verbunden.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
