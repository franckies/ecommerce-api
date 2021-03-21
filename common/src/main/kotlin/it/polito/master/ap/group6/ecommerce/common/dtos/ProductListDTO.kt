package it.polito.master.ap.group6.ecommerce.common.dtos

data class ProductListDTO(
    val products : MutableMap<ProductDTO, Int>
)