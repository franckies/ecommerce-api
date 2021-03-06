package it.polito.master.ap.group6.ecommerce.walletservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO

fun Wallet.toDto(): WalletDTO {
    return WalletDTO(this.userID, this.total, this.transactions?.map { it.toDto() })
}

fun RechargeDTO.toModel(): Transaction {
    return Transaction(null, null, this.userID, this.amount, this.timestamp, this.causal, null)
}

fun TransactionDTO.toModel(): Transaction {
    return Transaction(null, this.orderID, this.userID, this.amount, this.timestamp, null, this.status)
}

fun Transaction.toDto(): TransactionDTO {
    return TransactionDTO(this.userID, this.orderID, this.amount, this.timestamp, this.status)
}