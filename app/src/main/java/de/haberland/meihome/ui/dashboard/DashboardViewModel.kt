package de.haberland.meihome.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.haberland.meihome.data.auth.FirebaseAuthRepository
import de.haberland.meihome.data.lists.FirebaseListsRepository
import de.haberland.meihome.data.preferences.SharedPreferencesDashboardRepository
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = FirebaseAuthRepository()
    private val listsRepository = FirebaseListsRepository()
    private val preferencesRepository = SharedPreferencesDashboardRepository(application)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val _uiState = MutableStateFlow(preferencesRepository.dashboardState.value)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var listsJob: Job? = null
    private var shoppingItemsJob: Job? = null
    private var todoItemsJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.user.collectLatest { user ->
                crashlytics.setUserId(user?.uid.orEmpty())
                listsJob?.cancel()
                shoppingItemsJob?.cancel()
                todoItemsJob?.cancel()

                _uiState.value = _uiState.value.copy(
                    isSignedIn = user != null,
                    userEmail = user?.email,
                    availableLists = emptyList(),
                    shoppingItems = emptyList(),
                    todoItems = emptyList(),
                    errorMessage = null,
                )

                if (user != null) observeLists()
            }
        }
    }

    fun signIn(context: Context) {
        viewModelScope.launch {
            runCatching { authRepository.signIn(context) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Anmeldung fehlgeschlagen") }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            runCatching { authRepository.signOut(context) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Abmeldung fehlgeschlagen") }
        }
    }

    fun selectShoppingList(listId: String) {
        viewModelScope.launch {
            preferencesRepository.selectShoppingList(listId)
            _uiState.value = _uiState.value.copy(shoppingListId = listId)
            bindSelectedLists(_uiState.value.availableLists)
        }
    }

    fun selectTodoList(listId: String) {
        viewModelScope.launch {
            preferencesRepository.selectTodoList(listId)
            _uiState.value = _uiState.value.copy(todoListId = listId)
            bindSelectedLists(_uiState.value.availableLists)
        }
    }

    fun addShoppingItem(text: String) = addItem(_uiState.value.shoppingListId, text)
    fun addTodoItem(text: String) = addItem(_uiState.value.todoListId, text)

    fun setItemChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            runCatching { listsRepository.setItemChecked(itemId, checked) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Änderung fehlgeschlagen") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun addItem(listId: String?, text: String) {
        if (listId == null || text.isBlank()) return
        viewModelScope.launch {
            runCatching { listsRepository.addItem(listId, text) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Eintrag konnte nicht gespeichert werden") }
        }
    }

    private fun observeLists() {
        listsJob = viewModelScope.launch {
            listsRepository.observeAvailableLists().collectLatest { lists ->
                _uiState.value = _uiState.value.copy(availableLists = lists)
                bindSelectedLists(lists)
            }
        }
    }

    private fun bindSelectedLists(lists: List<MeiList>) {
        val shoppingId = _uiState.value.shoppingListId?.takeIf { id -> lists.any { it.id == id } }
        val todoId = _uiState.value.todoListId?.takeIf { id -> lists.any { it.id == id } }

        _uiState.value = _uiState.value.copy(
            shoppingListId = shoppingId,
            todoListId = todoId,
            shoppingListName = lists.firstOrNull { it.id == shoppingId }?.name,
            todoListName = lists.firstOrNull { it.id == todoId }?.name,
        )

        shoppingItemsJob?.cancel()
        shoppingItemsJob = shoppingId?.let { id ->
            viewModelScope.launch {
                listsRepository.observeItems(id).collectLatest { items ->
                    _uiState.value = _uiState.value.copy(shoppingItems = items.toUiItems())
                }
            }
        }

        todoItemsJob?.cancel()
        todoItemsJob = todoId?.let { id ->
            viewModelScope.launch {
                listsRepository.observeItems(id).collectLatest { items ->
                    _uiState.value = _uiState.value.copy(todoItems = items.toUiItems())
                }
            }
        }
    }

    private fun List<MeiListItem>.toUiItems(): List<DashboardListItemUiState> =
        map { item -> DashboardListItemUiState(item.id, item.text, item.isChecked) }
}
