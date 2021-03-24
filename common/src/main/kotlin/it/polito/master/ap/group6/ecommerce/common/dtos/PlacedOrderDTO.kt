package it.polito.master.ap.group6.ecommerce.common.dtos

data class PlacedOrderDTO(
    val user: UserDTO? = null,
    val purchaseList : List<PurchaseDTO>? = null,
    val deliveryAddress : String? = null
)

