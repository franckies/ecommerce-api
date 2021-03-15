package it.polito.master.ap.group6.ecommerce.common.dtos

data class OrderDTO(
    val orderID: String? = null,
    val purchases: List<PurchaseDTO>? = null
)
