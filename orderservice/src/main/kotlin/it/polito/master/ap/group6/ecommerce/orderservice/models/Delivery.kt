package it.polito.master.ap.group6.ecommerce.orderservice.models

import it.polito.master.ap.group6.ecommerce.common.dtos.ProductDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WarehouseDTO
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("deliveries")
class Delivery{
    @Id
    var id: String? = null
    val order: Order? = null
    val shippingAddress: String? = null
    val warehouse: WarehouseDTO? = null
    val products: MutableMap<ProductDTO,Int>? = null
}