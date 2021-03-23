package it.polito.master.ap.group6.ecommerce.orderservice.models

import it.polito.master.ap.group6.ecommerce.common.dtos.ProductDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WarehouseDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("deliveries")
class Delivery(
    val orderID: String? = null,
    val shippingAddress: String? = null,
    val warehouse: WarehouseDTO? = null,
    val products: List<Purchase>? = null,
    var status: DeliveryStatus? = null
) {
    @Id
    var id: String? = null
}