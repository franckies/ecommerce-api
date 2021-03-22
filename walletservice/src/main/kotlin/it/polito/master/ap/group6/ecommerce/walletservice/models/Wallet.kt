package it.polito.master.ap.group6.ecommerce.walletservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("wallets")
class Wallet (
    val user: UserDTO? = null,
    var total: Double? = null,
    val transactions: MutableList<Transaction>? = null
)