package it.polito.master.ap.group6.ecommerce.common.dtos

import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus

data class OrderDTO(
    val orderID: String? = null,
    val purchases: List<PurchaseDTO>? = null,
    val status: OrderStatus? = null
)
