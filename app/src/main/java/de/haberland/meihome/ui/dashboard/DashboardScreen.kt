package de.haberland.meihome.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.haberland.meihome.ui.dashboard.components.DashboardListCard
import de.haberland.meihome.ui.dashboard.components.SmartHomeBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggleShoppingItem: (String, Boolean) -> Unit,
    onToggleTodoItem: (String, Boolean) -> Unit,
    onSelectShoppingList: () -> Unit,
    onSelectTodoList: () -> Unit,
    onAddShoppingItem: () -> Unit,
    onAddTodoItem: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DashboardHeader(onOpenSettings = onOpenSettings)

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DashboardListCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "Einkauf",
                    selectedListName = state.shoppingListName,
                    items = state.shoppingItems,
                    onSelectList = onSelectShoppingList,
                    onAddItem = onAddShoppingItem,
                    onToggleItem = onToggleShoppingItem,
                )
                DashboardListCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = "Todos",
                    selectedListName = state.todoListName,
                    items = state.todoItems,
                    onSelectList = onSelectTodoList,
                    onAddItem = onAddTodoItem,
                    onToggleItem = onToggleTodoItem,
                )
            }

            CalendarCard(
                state = state,
                onRequestPermission = onRequestCalendarPermission,
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight(),
            )
        }

        SmartHomeBar()
    }
}

@Composable
private fun DashboardHeader(onOpenSettings: () -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(
                text = "MeiHome",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Familien-Dashboard",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen",
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(
    state: DashboardUiState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Kalender", style = MaterialTheme.typography.titleLarge)

            if (!state.calendarPermissionGranted) {
                Text(
                    "MeiHome kann die auf diesem Tablet synchronisierten Kalender anzeigen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestPermission) {
                    Text("Kalenderzugriff erlauben")
                }
                return@Column
            }

            if (state.calendarEvents.isEmpty()) {
                Text(
                    "Keine Termine in den nächsten drei Tagen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val grouped = state.calendarEvents.groupBy { event ->
                Instant.ofEpochMilli(event.startMillis).atZone(zone).toLocalDate()
            }

            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.toSortedMap().forEach { (date, events) ->
                    item(key = "header-" + date.toString()) {
                        Text(
                            text = when (date) {
                                today -> "Heute"
                                today.plusDays(1) -> "Morgen"
                                else -> date.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                        .withLocale(Locale.GERMAN),
                                )
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    items(
                        items = events,
                        key = { event -> event.id.toString() + "-" + event.startMillis.toString() },
                    ) { event ->
                        CalendarEntry(event = event)
                    }
                    item(key = "space-" + date.toString()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEntry(event: CalendarEventUiState) {
    val zone = ZoneId.systemDefault()
    val time = if (event.allDay) {
        "Ganztägig"
    } else {
        Instant.ofEpochMilli(event.startMillis)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            event.calendarName?.takeIf { it.isNotBlank() }?.let { calendar ->
                Text(
                    text = calendar,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
