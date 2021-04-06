package it.polito.master.ap.group6.ecommerce.common.dtos

data class DeliveryDTO(
    //val warehouse: WarehouseDTO? = null,
    val warehouseID: String? = null,
//    val delivery: Map<ProductDTO,Int>? = null
    val purchases : List<PurchaseDTO>? = null
)


data class DeliveryListDTO(
//    val order: OrderDTO? = null,
    val orderID : String? = null,
//    val deliveryList: MutableList<DeliveryDTO>? = null
    val deliveryList: List<DeliveryDTO>? = null,
    val deliveryAddress : String? = null
)