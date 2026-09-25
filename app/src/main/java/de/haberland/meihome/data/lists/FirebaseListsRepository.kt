package de.haberland.meihome.data.lists

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import de.haberland.meihome.domain.model.MeiList
import de.haberland.meihome.domain.model.MeiListItem
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
                    if (error != null || snapshot == null) return@addSnapshotListener
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
                if (error != null || snapshot == null) return@addSnapshotListener
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
        val listener = firestore.collection("list_items")
            .whereEqualTo("listId", listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.map { doc ->
                    MeiListItem(
                        id = doc.id,
                        listId = listId,
                        text = doc.getString("text").orEmpty(),
                        isChecked = doc.getBoolean("isChecked") ?: false,
                        area = doc.getString("area"),
                    )
                }.sortedWith(
                    compareBy<MeiListItem> { it.isChecked }
                        .thenBy { it.text.lowercase() },
                )
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addItem(listId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val id = UUID.randomUUID().toString()
        firestore.collection("list_items").document(id).set(
            mapOf(
                "listId" to listId,
                "text" to trimmed,
                "isChecked" to false,
                "timestamp" to System.currentTimeMillis(),
                "area" to null,
            ),
        ).await()
    }

    override suspend fun setItemChecked(itemId: String, checked: Boolean) {
        firestore.collection("list_items").document(itemId)
            .update("isChecked", checked)
            .await()
    }
}
