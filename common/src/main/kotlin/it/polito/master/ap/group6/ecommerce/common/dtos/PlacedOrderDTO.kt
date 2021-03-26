package it.polito.master.ap.group6.ecommerce.common.dtos

data class PlacedOrderDTO(
    val sagaID: String? = null,  // will be used as OrderID
    //val user: UserDTO? = null,
    val userID: String? = null,
    val purchaseList : List<PurchaseDTO>? = null,
    val deliveryAddress : String? = null
)

