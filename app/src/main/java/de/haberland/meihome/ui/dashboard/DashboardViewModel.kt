package de.haberland.meihome.ui.dashboard

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.haberland.meihome.data.auth.FirebaseAuthRepository
import de.haberland.meihome.data.calendar.AndroidCalendarRepository
import de.haberland.meihome.data.lists.FirebaseListsRepository
import de.haberland.meihome.data.preferences.SharedPreferencesDashboardRepository
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = FirebaseAuthRepository()
    private val calendarRepository = AndroidCalendarRepository(application)
    private val listsRepository = FirebaseListsRepository()
    private val preferencesRepository = SharedPreferencesDashboardRepository(application)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val _uiState = MutableStateFlow(preferencesRepository.dashboardState.value)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var listsJob: Job? = null
    private var shoppingItemsJob: Job? = null
    private var todoItemsJob: Job? = null
    private var calendarJob: Job? = null

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
                    shoppingListName = null,
                    todoListName = null,
                    shoppingItems = emptyList(),
                    todoItems = emptyList(),
                    errorMessage = null,
                )

                if (user != null) observeLists()
            }
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            runCatching { authRepository.signIn(activity) }
                .onFailure { reportError(it, "Anmeldung fehlgeschlagen") }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            runCatching { authRepository.signOut(context) }
                .onFailure { reportError(it, "Abmeldung fehlgeschlagen") }
        }
    }

    fun selectShoppingList(listId: String) {
        viewModelScope.launch {
            preferencesRepository.selectShoppingList(listId)
            _uiState.value = _uiState.value.copy(shoppingListId = listId, shoppingItems = emptyList())
            bindSelectedLists(_uiState.value.availableLists)
        }
    }

    fun selectTodoList(listId: String) {
        viewModelScope.launch {
            preferencesRepository.selectTodoList(listId)
            _uiState.value = _uiState.value.copy(todoListId = listId, todoItems = emptyList())
            bindSelectedLists(_uiState.value.availableLists)
        }
    }

    fun addTodoItem(text: String) = addItem(_uiState.value.todoListId, text)

    fun setItemChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            runCatching { listsRepository.setItemChecked(itemId, checked) }
                .onFailure { reportError(it, "Änderung fehlgeschlagen") }
        }
    }

    fun setCalendarPermission(granted: Boolean) {
        calendarJob?.cancel()
        _uiState.value = _uiState.value.copy(
            calendarPermissionGranted = granted,
            calendarEvents = if (granted) _uiState.value.calendarEvents else emptyList(),
        )
        if (granted) {
            calendarJob = viewModelScope.launch {
                calendarRepository.observeUpcomingEvents().collectLatest { events ->
                    _uiState.value = _uiState.value.copy(
                        calendarEvents = events.map { event ->
                            CalendarEventUiState(
                                id = event.id,
                                title = event.title,
                                startMillis = event.startMillis,
                                endMillis = event.endMillis,
                                allDay = event.allDay,
                                calendarName = event.calendarName,
                            )
                        },
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun addItem(listId: String?, text: String) {
        if (listId == null || text.isBlank()) return
        viewModelScope.launch {
            runCatching { listsRepository.addItem(listId, text) }
                .onFailure { reportError(it, "Eintrag konnte nicht gespeichert werden") }
        }
    }

    private fun observeLists() {
        listsJob = viewModelScope.launch {
            listsRepository.observeAvailableLists().catch { error ->
                bindSelectedLists(emptyList())
                reportError(error, "Listen konnten nicht geladen werden")
            }.collectLatest { lists ->
                bindSelectedLists(lists)
            }
        }
    }

    private fun bindSelectedLists(lists: List<MeiList>) {
        val shoppingId = _uiState.value.shoppingListId?.takeIf { id -> lists.any { it.id == id } }
        val todoId = _uiState.value.todoListId?.takeIf { id -> lists.any { it.id == id } }

        _uiState.value = _uiState.value.withAvailableLists(lists)

        shoppingItemsJob?.cancel()
        shoppingItemsJob = shoppingId?.let { id ->
            viewModelScope.launch {
                listsRepository.observeItems(id).catch { error ->
                    _uiState.value = _uiState.value.copy(shoppingItems = emptyList())
                    reportError(error, "Einkaufsliste konnte nicht geladen werden")
                }.collectLatest { items ->
                    _uiState.value = _uiState.value.copy(shoppingItems = items.toUiItems())
                }
            }
        }

        todoItemsJob?.cancel()
        todoItemsJob = todoId?.let { id ->
            viewModelScope.launch {
                listsRepository.observeItems(id).catch { error ->
                    _uiState.value = _uiState.value.copy(todoItems = emptyList())
                    reportError(error, "Todo-Liste konnte nicht geladen werden")
                }.collectLatest { items ->
                    _uiState.value = _uiState.value.copy(todoItems = items.toUiItems())
                }
            }
        }
    }

    private fun reportError(error: Throwable, fallback: String) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(errorMessage = fallback)
        crashlytics.recordException(error)
    }

    private fun List<MeiListItem>.toUiItems(): List<DashboardListItemUiState> =
        map { item -> DashboardListItemUiState(item.id, item.text, item.isChecked) }
}
