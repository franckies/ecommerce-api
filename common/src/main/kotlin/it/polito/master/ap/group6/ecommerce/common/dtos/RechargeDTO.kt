package it.polito.master.ap.group6.ecommerce.common.dtos

import java.util.*

data class RechargeDTO(
    //val user: UserDTO? = null,
    val userID: String? = null,
    val amount: Float? = null,
    val timestamp: Date? = null,
    val causal: String? = null,
    //TODO: UserDTO who charged is needed?
)
