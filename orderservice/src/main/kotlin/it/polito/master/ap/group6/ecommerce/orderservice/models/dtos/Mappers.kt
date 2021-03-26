package it.polito.master.ap.group6.ecommerce.orderservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PurchaseDTO
import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.Purchase

fun Order.toDto(): OrderDTO {
    return OrderDTO(this.id, this.purchases?.map { it.toDto() }, this.status)
}

fun OrderDTO.toModel(): Order {
    return Order(null, this.purchases?.map { it.toModel()!! }, this.status, null)
}

fun Purchase.toDto(): PurchaseDTO {
    return PurchaseDTO(this.productID, this.quantity, this.price)
}

fun PurchaseDTO.toModel(): Purchase {
    return Purchase(this.productID!!, this.quantity!!, this.sellingPrice!!)
}

fun PlacedOrderDTO.toModel(): Order {
    return Order(this.userID, this.purchaseList?.map { it.toModel()!! }, null, this.deliveryAddress)
}

fun Delivery.toDto(): DeliveryDTO {
    return DeliveryDTO(this.warehouse, this.products?.map { it.toDto() })
}