package de.haberland.meihome.domain.model

data class CatalogProduct(
    val id: String,
    val name: String,
    val defaultArea: String? = null,
)
