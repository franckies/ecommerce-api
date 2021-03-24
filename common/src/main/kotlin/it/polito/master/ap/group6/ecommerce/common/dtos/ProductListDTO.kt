package it.polito.master.ap.group6.ecommerce.common.dtos

data class ProductListDTO(
    //val products: MutableMap<ProductDTO, Int>
    val products: MutableList<ProductQuantityDTO>
)

data class ProductQuantityDTO(
    val product: ProductDTO? = null,
    val quantity: Int? = null
)