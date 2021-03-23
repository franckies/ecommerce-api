package it.polito.master.ap.group6.ecommerce.common.dtos

data class DeliveryDTO(
    val warehouse: WarehouseDTO? = null,
    val delivery: List<PurchaseDTO>? = null
)


data class DeliveryListDTO(
    val orderID: String? = null,
    val deliveryList: List<DeliveryDTO>? = null
)