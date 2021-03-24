package it.polito.master.ap.group6.ecommerce.walletservice.models.dtos

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO

fun Wallet.toDto(): WalletDTO {
    return WalletDTO(this.user, this.total, this.transactions?.map { it.toDto() })
}

fun RechargeDTO.toModel(): Transaction {
    return Transaction(null, this.user, this.amount, this.timestamp, this.causal, null)
}

fun TransactionDTO.toModel(): Transaction {
    return Transaction(null, this.user, this.amount, this.timestamp, this.causal, this.status)
}

fun Transaction.toDto(): TransactionDTO {
    return TransactionDTO(this.user, this.amount, this.timestamp, this.causal, this.status)
}