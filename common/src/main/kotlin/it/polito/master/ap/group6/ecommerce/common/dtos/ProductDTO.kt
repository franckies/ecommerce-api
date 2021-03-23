package it.polito.master.ap.group6.ecommerce.common.dtos

data class ProductDTO(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val picture: String? = null,
    val currentPrice: Float? = null
)

data class ProductListDTO(
    val products : Map<ProductDTO, Int>
)