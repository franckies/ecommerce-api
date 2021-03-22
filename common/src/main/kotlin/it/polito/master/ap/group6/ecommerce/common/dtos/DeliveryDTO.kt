package it.polito.master.ap.group6.ecommerce.common.dtos

data class DeliveryDTO(
    val warehouse: WarehouseDTO? = null,
    val delivery: Map<ProductDTO,Int>? = null
)


data class DeliveryListDTO(
    val order: OrderDTO? = null,
    val deliveryList: MutableList<DeliveryDTO>? = null
)