package it.polito.master.ap.group6.ecommerce.orderservice.repositories

import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository: MongoRepository<Order, ObjectId> {
    fun findByBuyerId(id: String): Optional<List<Order>>
}