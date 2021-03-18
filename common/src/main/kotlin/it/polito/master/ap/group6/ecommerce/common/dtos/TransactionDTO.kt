package it.polito.master.ap.group6.ecommerce.common.dtos

import java.util.*

data class TransactionDTO(
    val user: UserDTO? = null,
    val amount: Float? = null,
    val timestamp: Date? = null,
    val causal: String? = null,
    val purchases: List<PurchaseDTO>? = null
)
