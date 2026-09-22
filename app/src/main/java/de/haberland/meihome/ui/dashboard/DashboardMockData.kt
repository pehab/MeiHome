package de.haberland.meihome.ui.dashboard

object DashboardMockData {
    val state = DashboardUiState(
        shoppingListId = "mock-shopping",
        todoListId = "mock-todo",
        shoppingListName = "Einkauf",
        todoListName = "Familie",
        shoppingItems = listOf(
            DashboardListItemUiState("shopping-1", "Butter", false),
            DashboardListItemUiState("shopping-2", "Milch", false),
            DashboardListItemUiState("shopping-3", "Brot", true),
            DashboardListItemUiState("shopping-4", "Äpfel", false),
        ),
        todoItems = listOf(
            DashboardListItemUiState("todo-1", "Wäsche aufhängen", false),
            DashboardListItemUiState("todo-2", "Müll rausbringen", true),
            DashboardListItemUiState("todo-3", "Sporttasche packen", false),
        ),
    )
}
