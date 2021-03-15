package it.polito.master.ap.group6.ecommerce.common.dtos

data class WalletDTO(
    val user: UserDTO? = null,
    val total: Float? = null,
    val transactions: List<TransactionDTO>? = null
    //TODO: time is needed?
)
