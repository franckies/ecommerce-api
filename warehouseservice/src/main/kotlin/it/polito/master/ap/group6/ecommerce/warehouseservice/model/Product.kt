package it.polito.master.ap.group6.ecommerce.warehouseservice.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

data class WarehouseStock (
    var warehouseName : String? = null,
    var warehouseAddress : String? = null,
    var availableQuantity : Int? = null,
    var alarmLevel : Int? = null
        )

@Document
data class Product(
    @Id val id: ObjectId? = null,
    val name: String? = null,
    val category: String? = null,
    val currentPrice: Float? = null,
    val description: String? = null,
    val picture: String? = null,
    val stock : MutableList<WarehouseStock>? = null
)