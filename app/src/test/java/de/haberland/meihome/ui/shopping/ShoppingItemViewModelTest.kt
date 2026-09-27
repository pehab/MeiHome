package de.haberland.meihome.ui.shopping

import de.haberland.meihome.data.lists.ListsRepository
import de.haberland.meihome.domain.model.CatalogProduct
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingItemViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val list = MeiList("shopping", "groceries", "Einkauf")
    private val milk = CatalogProduct("milk", "Milch", "Kühlregal")
    private val repository = FakeListsRepository()
    private lateinit var model: ShoppingItemViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        model = ShoppingItemViewModel(repository)
    }

    @After fun tearDown() {
        model.reset()
        Dispatchers.resetMain()
    }

    @Test fun catalogSelectionUsesCanonicalNameAndAreaFromCorrectCategory() = runTest(dispatcher) {
        repository.products = listOf(milk)
        model.load(list)
        advanceUntilIdle()
        assertEquals("groceries", repository.requestedCategory)
        model.save("arbitrary typed text", "milk")
        advanceUntilIdle()
        assertEquals(listOf(SavedItem("shopping", "Milch", "Kühlregal")), repository.saved)
        assertTrue(model.state.value.saved)
    }

    @Test fun nonEmptyCatalogRejectsFreeTextAndUnknownProducts() = runTest(dispatcher) {
        repository.products = listOf(milk)
        model.load(list)
        advanceUntilIdle()
        model.save("Milch", null)
        model.save("Brot", "missing")
        advanceUntilIdle()
        assertTrue(repository.saved.isEmpty())
        assertNotNull(model.state.value.error)
    }

    @Test fun confirmedEmptyCatalogAllowsTrimmedFreeText() = runTest(dispatcher) {
        model.load(list)
        advanceUntilIdle()
        model.save("  Brot  ", null)
        advanceUntilIdle()
        assertEquals(listOf(SavedItem("shopping", "Brot", null)), repository.saved)
    }

    @Test fun loadingAndLoadFailureDoNotEnableFreeText() = runTest(dispatcher) {
        repository.loadError = IllegalStateException("permission denied")
        model.load(list)
        model.save("Brot", null)
        advanceUntilIdle()
        model.save("Brot", null)
        advanceUntilIdle()
        assertNull(model.state.value.products)
        assertNotNull(model.state.value.error)
        assertTrue(repository.saved.isEmpty())
        repository.loadError = null
        model.load(list)
        advanceUntilIdle()
        assertEquals(emptyList<CatalogProduct>(), model.state.value.products)
        assertNull(model.state.value.error)
    }

    @Test fun repeatedSaveClicksOnlyWriteOnce() = runTest(dispatcher) {
        model.load(list)
        advanceUntilIdle()
        model.save("Brot", null)
        model.save("Brot", null)
        advanceUntilIdle()
        model.save("Brot", null)
        assertEquals(1, repository.saved.size)
    }

    @Test fun failedSaveKeepsCatalogAndAllowsRetry() = runTest(dispatcher) {
        repository.products = listOf(milk)
        repository.saveError = IllegalStateException("write denied")
        model.load(list)
        advanceUntilIdle()
        model.save("", "milk")
        advanceUntilIdle()
        assertFalse(model.state.value.saved)
        assertFalse(model.state.value.saving)
        assertEquals(listOf(milk), model.state.value.products)
        assertNotNull(model.state.value.error)
        repository.saveError = null
        model.save("", "milk")
        advanceUntilIdle()
        assertTrue(model.state.value.saved)
    }

    @Test fun switchingListsCancelsOldLoadAndUsesNewCategory() = runTest(dispatcher) {
        val delayed = CompletableDeferred<List<CatalogProduct>>()
        repository.delayedCatalog = delayed
        model.load(list)
        runCurrent()
        repository.delayedCatalog = null
        repository.products = listOf(CatalogProduct("bread", "Brot"))
        val other = MeiList("other", "bakery", "Bäckerei")
        model.load(other)
        advanceUntilIdle()
        delayed.complete(listOf(milk))
        advanceUntilIdle()
        assertEquals(other, model.state.value.list)
        assertEquals("bakery", repository.requestedCategory)
        assertEquals("bread", model.state.value.products!!.single().id)
        model.save("", "bread")
        advanceUntilIdle()
        assertEquals("other", repository.saved.single().listId)
    }

    @Test fun closingDialogCancelsLoadWithoutDisplayingCancellationError() = runTest(dispatcher) {
        repository.delayedCatalog = CompletableDeferred()
        model.load(list)
        runCurrent()
        model.reset()
        advanceUntilIdle()
        assertEquals(ShoppingItemState(), model.state.value)
    }

    @Test fun blankFreeTextIsNotSaved() = runTest(dispatcher) {
        model.load(list)
        advanceUntilIdle()
        model.save("   ", null)
        advanceUntilIdle()
        assertTrue(repository.saved.isEmpty())
    }
}

private data class SavedItem(val listId: String, val text: String, val area: String?)

private class FakeListsRepository : ListsRepository {
    var products = emptyList<CatalogProduct>()
    var requestedCategory: String? = null
    var loadError: Exception? = null
    var saveError: Exception? = null
    var delayedCatalog: CompletableDeferred<List<CatalogProduct>>? = null
    val saved = mutableListOf<SavedItem>()

    override suspend fun loadCatalog(categoryId: String): List<CatalogProduct> {
        requestedCategory = categoryId
        loadError?.let { throw it }
        return delayedCatalog?.await() ?: products
    }

    override suspend fun addItem(listId: String, text: String, area: String?) {
        saveError?.let { throw it }
        saved += SavedItem(listId, text, area)
    }

    override fun observeAvailableLists(): Flow<List<MeiList>> = flowOf(emptyList())
    override fun observeItems(listId: String): Flow<List<MeiListItem>> = flowOf(emptyList())
    override suspend fun setItemChecked(itemId: String, checked: Boolean) = Unit
}
