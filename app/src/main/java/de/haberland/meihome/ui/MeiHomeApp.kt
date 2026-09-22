package de.haberland.meihome.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.haberland.meihome.ui.dashboard.DashboardMockData
import de.haberland.meihome.ui.dashboard.DashboardScreen

@Composable
fun MeiHomeApp() {
    var state by remember { mutableStateOf(DashboardMockData.state) }

    MaterialTheme(
        colorScheme = darkColorScheme(),
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
