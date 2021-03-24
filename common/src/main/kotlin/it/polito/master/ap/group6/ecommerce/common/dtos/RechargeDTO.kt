package it.polito.master.ap.group6.ecommerce.common.dtos

import java.util.*

data class RechargeDTO(
    val user: UserDTO? = null,
    val amount: Double? = null,
    val timestamp: Calendar? = null,
    val causal: String? = null,
    //TODO: UserDTO who charged is needed?
)
