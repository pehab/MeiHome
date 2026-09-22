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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
    state: DashboardUiState = DashboardUiState(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DashboardHeader()

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DashboardListCard(
                    modifier = Modifier.weight(1f),
                    title = "Einkauf",
                    selectedListName = state.shoppingListName,
                    items = state.shoppingItems,
                    onSelectList = {},
                    onAddItem = {},
                    onToggleItem = { _, _ -> },
                )
                DashboardListCard(
                    modifier = Modifier.weight(1f),
                    title = "Todos",
                    selectedListName = state.todoListName,
                    items = state.todoItems,
                    onSelectList = {},
                    onAddItem = {},
                    onToggleItem = { _, _ -> },
                )
            }

            CalendarCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        SmartHomeBar()
    }
}

@Composable
private fun DashboardHeader() {
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
        }
    }
}

@Composable
private fun CalendarCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Kalender", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Android-Kalender wird als eigener Datenbereich angebunden.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
