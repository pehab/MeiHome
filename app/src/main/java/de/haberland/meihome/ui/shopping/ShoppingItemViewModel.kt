package de.haberland.meihome.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.haberland.meihome.data.lists.FirebaseListsRepository
import de.haberland.meihome.data.lists.ListsRepository
import de.haberland.meihome.domain.model.CatalogProduct
import de.haberland.meihome.domain.model.MeiList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingItemState(
    val list: MeiList? = null,
    // null means unknown; an empty list means the server confirmed no products.
    val products: List<CatalogProduct>? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class ShoppingItemViewModel(
    private val repository: ListsRepository = FirebaseListsRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ShoppingItemState())
    val state = mutableState.asStateFlow()
    private var operation: Job? = null

    fun load(list: MeiList) {
        if (mutableState.value.saving) return
        operation?.cancel()
        mutableState.value = ShoppingItemState(list = list, loading = true)
        operation = viewModelScope.launch {
            try {
                val products = repository.loadCatalog(list.categoryId)
                mutableState.value = ShoppingItemState(list = list, products = products)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = ShoppingItemState(
                    list = list,
                    error = "Katalog konnte nicht geladen werden. Bitte Verbindung und Zugriffsrechte prüfen.",
                )
            }
        }
    }

    fun save(text: String, productId: String?) {
        val current = mutableState.value
        val list = current.list ?: return
        val products = current.products ?: return
        if (current.loading || current.saving || current.saved) return

        val product = products.firstOrNull { it.id == productId }
        if (products.isNotEmpty() && product == null) {
            mutableState.value = current.copy(error = "Bitte ein Produkt aus dem Katalog auswählen.")
            return
        }
        val name = product?.name ?: text.trim()
        if (name.isBlank()) return

        mutableState.value = current.copy(saving = true, error = null)
        operation = viewModelScope.launch {
            try {
                repository.addItem(list.id, name, product?.defaultArea)
                mutableState.value = mutableState.value.copy(saving = false, saved = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false,
                    error = "Eintrag konnte nicht gespeichert werden. Bitte erneut versuchen.",
                )
            }
        }
    }

    fun reset() {
        operation?.cancel()
        mutableState.value = ShoppingItemState()
    }
}
