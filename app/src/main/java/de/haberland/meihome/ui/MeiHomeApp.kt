package de.haberland.meihome.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.haberland.meihome.ui.dashboard.DashboardMockData
import de.haberland.meihome.ui.dashboard.DashboardScreen

@Composable
fun MeiHomeApp() {
    var state by remember { mutableStateOf(DashboardMockData.state) }

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
    }
}
