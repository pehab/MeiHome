package de.haberland.meihome.data.lists

import de.haberland.meihome.domain.model.CatalogProduct
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import kotlinx.coroutines.flow.Flow

interface ListsRepository {
    fun observeAvailableLists(): Flow<List<MeiList>>
    fun observeItems(listId: String): Flow<List<MeiListItem>>
    suspend fun loadCatalog(categoryId: String): List<CatalogProduct>
    suspend fun addItem(listId: String, text: String, area: String? = null)
    suspend fun setItemChecked(itemId: String, checked: Boolean)
}
