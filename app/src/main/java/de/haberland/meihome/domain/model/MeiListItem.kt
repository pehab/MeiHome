package de.haberland.meihome.domain.model

data class MeiListItem(
    val id: String,
    val listId: String,
    val text: String,
    val isChecked: Boolean,
    val area: String?,
)
