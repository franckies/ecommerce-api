package it.polito.master.ap.group6.ecommerce.common.dtos

//needed when the admin wants to modify the product
data class ProductAdminDTO(
    val product: ProductDTO? = null,
    val warehouse: WarehouseDTO? = null,
//    val warehouseID: String? = null,
    val alarmLevel: Int? = null,
    val warehouseQuantity: Int? = null //quantity of that product in this warehouse
)

