package it.polito.master.ap.group6.ecommerce.walletservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionStatus
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("transactions")
class Transaction (
    @Id
    var id: String? = null,
    val user: UserDTO? = null,
    val amount: Double? = null,
    val timestamp: Calendar? = null,
    val causal: String? = null,
    var status: TransactionStatus? = null
)