package it.polito.master.ap.group6.ecommerce.walletservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("wallets")
class Wallet (
    @Id
    var id: String? = null,
    //val user: UserDTO? = null,
    val userID: String? = null,
    var total: Float? = null,
    val transactions: MutableList<Transaction>? = null
)