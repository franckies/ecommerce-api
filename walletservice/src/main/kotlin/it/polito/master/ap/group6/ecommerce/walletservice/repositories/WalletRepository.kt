package it.polito.master.ap.group6.ecommerce.walletservice.repositories

import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface WalletRepository: MongoRepository<Wallet, ObjectId> {
    fun findByUserID(id: String): Wallet
}
