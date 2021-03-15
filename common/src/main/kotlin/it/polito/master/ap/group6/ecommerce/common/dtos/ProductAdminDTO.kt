package it.polito.master.ap.group6.ecommerce.common.dtos

data class ProductAdminDTO(
    val product: ProductDTO? = null,
    val warehouse: WarehouseDTO? = null,
    val alarmLevel: Int? = null,
    val warehouseQuantity: Int? = null //quantity of that product in this warehouse
)

data class ProductAdminListDTO(
    val productList: List<ProductAdminDTO>? = null
)