package it.polito.master.ap.group6.ecommerce.common.dtos

import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import java.util.*

data class TransactionDTO(
    //val user: UserDTO? = null,
    val userID: String? = null,
    val amount: Float? = null,
    val timestamp: Date? = null,
    val causal: String? = null,
    val status: TransactionStatus? = null
)
