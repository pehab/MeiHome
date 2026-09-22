package de.haberland.meihome.ui.dashboard

data class DashboardUiState(
    val shoppingListId: String? = null,
    val todoListId: String? = null,
    val shoppingListName: String? = null,
    val todoListName: String? = null,
    val shoppingItems: List<DashboardListItemUiState> = emptyList(),
    val todoItems: List<DashboardListItemUiState> = emptyList(),
)

data class DashboardListItemUiState(
    val id: String,
    val text: String,
    val isChecked: Boolean,
)
