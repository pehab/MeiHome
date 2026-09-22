package de.haberland.meihome.ui.dashboard

import de.haberland.meihome.domain.model.MeiList

data class DashboardUiState(
    val shoppingList: MeiList? = null,
    val todoList: MeiList? = null,
)
