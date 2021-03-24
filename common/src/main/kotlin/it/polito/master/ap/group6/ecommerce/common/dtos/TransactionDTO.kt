package it.polito.master.ap.group6.ecommerce.common.dtos

import java.util.*

data class TransactionDTO(
    val user: UserDTO? = null,
    val amount: Double? = null,
    val timestamp: Calendar? = null,
    val causal: String? = null,
    val status: TransactionStatus? = null
)

enum class TransactionStatus {
    PENDING,
    ACCEPTED,
    REFUSED
}