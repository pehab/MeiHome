package de.haberland.meihome.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.ui.dashboard.DashboardScreen
import de.haberland.meihome.ui.dashboard.DashboardViewModel

private enum class ListRole {
    SHOPPING,
    TODO,
}

@Composable
fun MeiHomeApp(
    updateReadyToInstall: Boolean = false,
    onInstallUpdate: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var updatePromptDismissed by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var selectingRole by remember { mutableStateOf<ListRole?>(null) }
    var addRole by remember { mutableStateOf<ListRole?>(null) }

    LaunchedEffect(updateReadyToInstall) {
        if (!updateReadyToInstall) updatePromptDismissed = false
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            DashboardScreen(
                state = state,
                onToggleShoppingItem = viewModel::setItemChecked,
                onToggleTodoItem = viewModel::setItemChecked,
                onSelectShoppingList = { selectingRole = ListRole.SHOPPING },
                onSelectTodoList = { selectingRole = ListRole.TODO },
                onAddShoppingItem = { addRole = ListRole.SHOPPING },
                onAddTodoItem = { addRole = ListRole.TODO },
                onOpenSettings = { settingsOpen = true },
            )
        }

        if (settingsOpen) {
            SettingsDialog(
                isSignedIn = state.isSignedIn,
                userEmail = state.userEmail,
                shoppingListName = state.shoppingListName,
                todoListName = state.todoListName,
                onSignIn = { viewModel.signIn(context) },
                onSignOut = { viewModel.signOut(context) },
                onSelectShopping = { selectingRole = ListRole.SHOPPING },
                onSelectTodo = { selectingRole = ListRole.TODO },
                onDismiss = { settingsOpen = false },
            )
        }

        selectingRole?.let { role ->
            ListSelectionDialog(
                title = if (role == ListRole.SHOPPING) "Einkaufsliste auswählen" else "Todo-Liste auswählen",
                lists = state.availableLists,
                selectedId = if (role == ListRole.SHOPPING) state.shoppingListId else state.todoListId,
                onSelect = { id ->
                    if (role == ListRole.SHOPPING) viewModel.selectShoppingList(id) else viewModel.selectTodoList(id)
                    selectingRole = null
                },
                onDismiss = { selectingRole = null },
            )
        }

        addRole?.let { role ->
            AddItemDialog(
                title = if (role == ListRole.SHOPPING) "Einkauf hinzufügen" else "Todo hinzufügen",
                onAdd = { text ->
                    if (role == ListRole.SHOPPING) viewModel.addShoppingItem(text) else viewModel.addTodoItem(text)
                    addRole = null
                },
                onDismiss = { addRole = null },
            )
        }

        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                title = { Text("Fehler") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK")
                    }
                },
            )
        }

        if (updateReadyToInstall && !updatePromptDismissed) {
            AlertDialog(
                onDismissRequest = { updatePromptDismissed = true },
                title = { Text("Update bereit") },
                text = { Text("Eine neue Version von MeiHome wurde heruntergeladen und kann jetzt installiert werden.") },
                confirmButton = {
                    TextButton(onClick = onInstallUpdate) {
                        Text("Installieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updatePromptDismissed = true }) {
                        Text("Später")
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    isSignedIn: Boolean,
    userEmail: String?,
    shoppingListName: String?,
    todoListName: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSelectShopping: () -> Unit,
    onSelectTodo: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MeiHome Einstellungen") },
        text = {
            Column {
                Text(
                    if (isSignedIn) {
                        "Angemeldet als " + (userEmail ?: "Google-Konto")
                    } else {
                        "Nicht angemeldet"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (isSignedIn) {
                    TextButton(onClick = onSelectShopping) {
                        Text("Einkauf: " + (shoppingListName ?: "Liste auswählen"))
                    }
                    TextButton(onClick = onSelectTodo) {
                        Text("Todos: " + (todoListName ?: "Liste auswählen"))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(onClick = onSignOut) {
                        Text("Abmelden")
                    }
                } else {
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Mit Google anmelden")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
    )
}

@Composable
private fun ListSelectionDialog(
    title: String,
    lists: List<MeiList>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (lists.isEmpty()) {
                Text("Keine freigegebenen MeiLists-Listen gefunden.")
            } else {
                LazyColumn {
                    items(lists, key = { it.id }) { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(list.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = list.id == selectedId,
                                onClick = { onSelect(list.id) },
                            )
                            Text(list.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
private fun AddItemDialog(
    title: String,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Eintrag") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
