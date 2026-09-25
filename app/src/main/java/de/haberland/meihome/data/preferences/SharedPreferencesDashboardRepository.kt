package de.haberland.meihome.data.preferences

import android.content.Context
import de.haberland.meihome.ui.dashboard.DashboardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedPreferencesDashboardRepository(context: Context) : DashboardPreferencesRepository {
    private val preferences = context.getSharedPreferences("dashboard", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(
        DashboardUiState(
            shoppingListId = preferences.getString(KEY_SHOPPING_LIST, null),
            todoListId = preferences.getString(KEY_TODO_LIST, null),
        ),
    )

    override val dashboardState: StateFlow<DashboardUiState> = state

    override suspend fun selectShoppingList(listId: String) {
        preferences.edit().putString(KEY_SHOPPING_LIST, listId).apply()
        state.value = state.value.copy(shoppingListId = listId)
    }

    override suspend fun selectTodoList(listId: String) {
        preferences.edit().putString(KEY_TODO_LIST, listId).apply()
        state.value = state.value.copy(todoListId = listId)
    }

    companion object {
        private const val KEY_SHOPPING_LIST = "shopping_list_id"
        private const val KEY_TODO_LIST = "todo_list_id"
    }
}
