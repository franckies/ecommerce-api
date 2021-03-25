package it.polito.master.ap.group6.ecommerce.walletservice.repositories

import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Transaction
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository: MongoRepository<Transaction, ObjectId> {
    fun findById(id: String): Transaction
    fun findByCausal(id: String): Transaction
}
