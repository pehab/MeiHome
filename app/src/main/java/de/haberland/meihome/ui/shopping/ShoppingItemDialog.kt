package de.haberland.meihome.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.haberland.meihome.domain.model.MeiList

@Composable
fun ShoppingItemDialog(
    list: MeiList,
    onDismiss: () -> Unit,
    model: ShoppingItemViewModel = viewModel(),
) {
    val state by model.state.collectAsStateWithLifecycle()
    var query by remember(list.id) { mutableStateOf("") }
    var selectedId by remember(list.id) { mutableStateOf<String?>(null) }
    val products = state.products
    val hasCatalog = !products.isNullOrEmpty()
    val ready = state.list == list && products != null && !state.loading && !state.saving && !state.saved

    LaunchedEffect(list) { model.load(list) }
    LaunchedEffect(state.saved) { if (state.saved) onDismiss() }
    DisposableEffect(model) { onDispose { model.reset() } }

    AlertDialog(
        onDismissRequest = { if (!state.saving) onDismiss() },
        title = { Text("Einkauf hinzufügen") },
        text = {
            Column {
                if (state.loading) {
                    Text("Katalog wird geladen …")
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!state.loading && products == null) {
                    TextButton(onClick = { model.load(list) }) { Text("Erneut versuchen") }
                }
                if (products != null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; selectedId = null },
                        label = { Text(if (hasCatalog) "Produkt suchen" else "Eintrag") },
                        singleLine = true,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (hasCatalog) {
                        val matches = products.filter { it.name.contains(query.trim(), ignoreCase = true) }
                        if (matches.isEmpty()) Text("Kein passendes Produkt im Katalog.")
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(matches, key = { it.id }) { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = !state.saving) { selectedId = product.id }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selectedId == product.id,
                                        enabled = !state.saving,
                                        onClick = { selectedId = product.id },
                                    )
                                    Column {
                                        Text(product.name)
                                        product.defaultArea?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { model.save(query, selectedId) },
                enabled = ready && if (hasCatalog) products.orEmpty().any { it.id == selectedId } else query.isNotBlank(),
            ) { Text(if (state.saving) "Wird gespeichert …" else "Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.saving) { Text("Abbrechen") }
        },
    )
}
