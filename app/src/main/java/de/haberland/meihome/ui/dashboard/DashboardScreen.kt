package de.haberland.meihome.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haberland.meihome.ui.dashboard.components.DashboardListCard
import de.haberland.meihome.ui.dashboard.components.SmartHomeBar

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggleShoppingItem: (String, Boolean) -> Unit,
    onToggleTodoItem: (String, Boolean) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DashboardHeader(onOpenSettings = onOpenSettings)

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DashboardListCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "Einkauf",
                    selectedListName = state.shoppingListName,
                    items = state.shoppingItems,
                    onSelectList = {},
                    onAddItem = {},
                    onToggleItem = onToggleShoppingItem,
                )
                DashboardListCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "Todos",
                    selectedListName = state.todoListName,
                    items = state.todoItems,
                    onSelectList = {},
                    onAddItem = {},
                    onToggleItem = onToggleTodoItem,
                )
            }

            CalendarCard(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
            )
        }

        SmartHomeBar()
    }
}

@Composable
private fun DashboardHeader(onOpenSettings: () -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(
                text = "MeiHome",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Familien-Dashboard",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen",
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Kalender", style = MaterialTheme.typography.titleLarge)
            Text("Heute", style = MaterialTheme.typography.labelLarge)
            CalendarEntry("16:00", "Fußball")
            CalendarEntry("18:30", "Elternabend")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Morgen", style = MaterialTheme.typography.labelLarge)
            CalendarEntry("08:00", "Schule")
            CalendarEntry("17:15", "Training")
        }
    }
}

@Composable
private fun CalendarEntry(
    time: String,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
