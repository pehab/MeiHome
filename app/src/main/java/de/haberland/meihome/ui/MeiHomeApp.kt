package de.haberland.meihome.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.haberland.meihome.ui.dashboard.DashboardMockData
import de.haberland.meihome.ui.dashboard.DashboardScreen

@Composable
fun MeiHomeApp(
    updateReadyToInstall: Boolean = false,
    onInstallUpdate: () -> Unit = {},
) {
    var state by remember { mutableStateOf(DashboardMockData.state) }
    var updatePromptDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(updateReadyToInstall) {
        if (!updateReadyToInstall) updatePromptDismissed = false
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            DashboardScreen(
                state = state,
                onToggleShoppingItem = { itemId, checked ->
                    state = state.copy(
                        shoppingItems = state.shoppingItems.map { item ->
                            if (item.id == itemId) item.copy(isChecked = checked) else item
                        },
                    )
                },
                onToggleTodoItem = { itemId, checked ->
                    state = state.copy(
                        todoItems = state.todoItems.map { item ->
                            if (item.id == itemId) item.copy(isChecked = checked) else item
                        },
                    )
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
