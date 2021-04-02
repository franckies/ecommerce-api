package it.polito.master.ap.group6.ecommerce.common.dtos

import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus

data class MailingInfoDTO (
    val userId: String? = null,
    val orderStatus: OrderStatus? = null,
    val orderId: String? = null,
    val message: String? = null
)