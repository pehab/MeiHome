package de.haberland.meihome.data.lists

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import de.haberland.meihome.domain.model.CatalogProduct
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private data class RemoteListItem(
    val id: String,
    val text: String,
    val isChecked: Boolean,
    val area: String?,
    val repeatEveryDays: Int?,
    val nextDueAt: Long?,
)

class FirebaseListsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ListsRepository {

    override fun observeAvailableLists(): Flow<List<MeiList>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listListeners = mutableMapOf<String, ListenerRegistration>()
        val lists = linkedMapOf<String, MeiList>()

        fun publish() {
            trySend(lists.values.sortedBy { it.name.lowercase() })
        }

        fun attachLists(categoryId: String) {
            if (listListeners.containsKey(categoryId)) return
            listListeners[categoryId] = firestore.collection("shopping_lists")
                .whereEqualTo("categoryId", categoryId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    snapshot.documentChanges.forEach { change ->
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                lists[change.document.id] = MeiList(
                                    id = change.document.id,
                                    categoryId = categoryId,
                                    name = change.document.getString("name").orEmpty(),
                                )
                            }
                            DocumentChange.Type.REMOVED -> lists.remove(change.document.id)
                        }
                    }
                    publish()
                }
        }

        val categoriesListener = firestore.collection("categories")
            .whereArrayContains("allowedUsers", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                if (snapshot.isEmpty) publish()
                snapshot.documentChanges.forEach { change ->
                    val categoryId = change.document.id
                    when (change.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> attachLists(categoryId)
                        DocumentChange.Type.REMOVED -> {
                            listListeners.remove(categoryId)?.remove()
                            lists.entries.removeAll { it.value.categoryId == categoryId }
                            publish()
                        }
                    }
                }
            }

        awaitClose {
            categoriesListener.remove()
            listListeners.values.forEach { it.remove() }
        }
    }

    override fun observeItems(listId: String): Flow<List<MeiListItem>> = callbackFlow {
        var remoteItems = emptyList<RemoteListItem>()

        fun publish() {
            val now = System.currentTimeMillis()
            val items = remoteItems.map { item ->
                val recurrenceDue = item.repeatEveryDays != null &&
                    item.nextDueAt != null &&
                    item.nextDueAt <= now
                MeiListItem(
                    id = item.id,
                    listId = listId,
                    text = item.text,
                    isChecked = item.isChecked && !recurrenceDue,
                    area = item.area,
                )
            }.sortedWith(
                compareBy<MeiListItem> { it.isChecked }
                    .thenBy { it.text.lowercase() },
            )
            trySend(items)
        }

        val listener = firestore.collection("list_items")
            .whereEqualTo("listId", listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                remoteItems = snapshot.documents.map { doc ->
                    RemoteListItem(
                        id = doc.id,
                        text = doc.getString("text").orEmpty(),
                        isChecked = doc.getBoolean("isChecked") ?: false,
                        area = doc.getString("area"),
                        repeatEveryDays = doc.getLong("repeatEveryDays")
                            ?.takeIf { it in 1L..3650L }
                            ?.toInt(),
                        nextDueAt = doc.getLong("nextDueAt"),
                    )
                }
                publish()
            }

        // A due recurrence does not change the Firestore document itself, so refresh
        // the effective checked state while the wall tablet stays open.
        val recurrenceClock = launch {
            while (true) {
                delay(15_000)
                publish()
            }
        }

        awaitClose {
            recurrenceClock.cancel()
            listener.remove()
        }
    }

    override suspend fun loadCatalog(categoryId: String): List<CatalogProduct> =
        firestore.collection("catalog_products")
            .whereEqualTo("categoryId", categoryId)
            // An empty local cache must not be mistaken for a category without a catalog.
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { document ->
                val name = document.getString("name")?.trim().orEmpty()
                if (name.isBlank()) null else CatalogProduct(
                    id = document.id,
                    name = name,
                    defaultArea = document.getString("defaultArea"),
                )
            }.sortedBy { it.name.lowercase() }

    override suspend fun addItem(listId: String, text: String, area: String?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val id = UUID.randomUUID().toString()
        firestore.collection("list_items").document(id).set(
            mapOf(
                "listId" to listId,
                "text" to trimmed,
                "isChecked" to false,
                "timestamp" to System.currentTimeMillis(),
                "area" to area,
            ),
        ).await()
    }

    override suspend fun setItemChecked(itemId: String, checked: Boolean) {
        val document = firestore.collection("list_items").document(itemId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            val repeatEveryDays = snapshot.getLong("repeatEveryDays")
                ?.takeIf { it in 1L..3650L }
                ?.toInt()
            val nextDueAt = if (checked && repeatEveryDays != null) {
                nextRepeatDueAt(repeatEveryDays, System.currentTimeMillis())
            } else {
                null
            }

            transaction.update(
                document,
                mapOf(
                    "isChecked" to checked,
                    "nextDueAt" to nextDueAt,
                ),
            )
        }.await()
    }

    private fun nextRepeatDueAt(days: Int, completedAt: Long): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(completedAt)
            .atZone(zone)
            .toLocalDate()
            .plusDays(days.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

}
