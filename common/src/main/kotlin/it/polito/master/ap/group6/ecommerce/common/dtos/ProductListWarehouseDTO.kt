package it.polito.master.ap.group6.ecommerce.common.dtos


data class WarehouseStockDTO (
    var warehouseName : String? = null,
    var warehouseAddress : String? = null,
    var availableQuantity : Int? = null,
    var alarmLevel : Int? = null
)

data class ProductWarehouseDTO(
    val id: String? = null,
    val name: String? = null,
    val category: String? = null,
    val currentPrice: Float? = null,
    val description: String? = null,
    val picture: String? = null,
    val stock : MutableList<WarehouseStockDTO>? = null
)

data class ProductListWarehouseDTO(
    val products : MutableList<ProductWarehouseDTO>? = null
)
