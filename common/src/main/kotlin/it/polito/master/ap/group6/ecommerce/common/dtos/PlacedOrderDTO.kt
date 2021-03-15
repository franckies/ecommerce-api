package it.polito.master.ap.group6.ecommerce.common.dtos

data class PlacedOrderDTO(
    val user: UserDTO? = null,
    val purchaseList : List<PurchaseDTO>? = null,
    val deliveryAddress : String? = null
)

data class PlacedOrderListDTO(
    val placedOrderList : Map<String, List<PurchaseDTO>>? = null
)
