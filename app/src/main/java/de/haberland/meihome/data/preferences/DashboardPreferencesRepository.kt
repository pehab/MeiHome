package de.haberland.meihome.data.preferences

import de.haberland.meihome.ui.dashboard.DashboardUiState
import kotlinx.coroutines.flow.StateFlow

interface DashboardPreferencesRepository {
    val dashboardState: StateFlow<DashboardUiState>

    suspend fun selectShoppingList(listId: String)
    suspend fun selectTodoList(listId: String)
}
