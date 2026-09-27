package de.haberland.meihome.ui.dashboard

import de.haberland.meihome.domain.model.MeiList

data class DashboardUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val availableLists: List<MeiList> = emptyList(),
    val shoppingListId: String? = null,
    val todoListId: String? = null,
    val shoppingListName: String? = null,
    val todoListName: String? = null,
    val shoppingItems: List<DashboardListItemUiState> = emptyList(),
    val todoItems: List<DashboardListItemUiState> = emptyList(),
    val calendarPermissionGranted: Boolean = false,
    val calendarEvents: List<CalendarEventUiState> = emptyList(),
    val errorMessage: String? = null,
)

data class DashboardListItemUiState(
    val id: String,
    val text: String,
    val isChecked: Boolean,
)

data class CalendarEventUiState(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String?,
)

/** Keep the user's selection while category listeners deliver their snapshots independently. */
internal fun DashboardUiState.withAvailableLists(lists: List<MeiList>): DashboardUiState {
    val shopping = lists.firstOrNull { it.id == shoppingListId }
    val todo = lists.firstOrNull { it.id == todoListId }
    return copy(
        availableLists = lists,
        shoppingListName = shopping?.name,
        todoListName = todo?.name,
        shoppingItems = if (shopping == null) emptyList() else shoppingItems,
        todoItems = if (todo == null) emptyList() else todoItems,
    )
}
