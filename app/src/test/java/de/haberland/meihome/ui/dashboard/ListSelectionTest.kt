package de.haberland.meihome.ui.dashboard

import de.haberland.meihome.domain.model.MeiList
import org.junit.Assert.*
import org.junit.Test

class ListSelectionTest {
    @Test fun laterCategorySnapshotRestoresSavedSelection() {
        val saved = DashboardUiState(shoppingListId = "shopping", todoListId = "tasks")
        val tasks = MeiList("tasks", "family", "Aufgaben")
        val shopping = MeiList("shopping", "groceries", "Einkauf")
        val partial = saved.withAvailableLists(listOf(tasks))
        assertEquals("shopping", partial.shoppingListId)
        assertNull(partial.shoppingListName)
        assertEquals("Aufgaben", partial.todoListName)
        val complete = partial.withAvailableLists(listOf(tasks, shopping))
        assertEquals("shopping", complete.shoppingListId)
        assertEquals("Einkauf", complete.shoppingListName)
    }

    @Test fun missingListClearsStaleContentWithoutOverwritingPreference() {
        val state = DashboardUiState(
            shoppingListId = "shopping",
            shoppingListName = "Einkauf",
            shoppingItems = listOf(DashboardListItemUiState("milk", "Milch", false)),
        ).withAvailableLists(emptyList())
        assertEquals("shopping", state.shoppingListId)
        assertNull(state.shoppingListName)
        assertTrue(state.shoppingItems.isEmpty())
    }
}
