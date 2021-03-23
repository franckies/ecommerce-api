package it.polito.master.ap.group6.ecommerce.orderservice.repositories

import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DeliveryRepository : MongoRepository<Delivery, ObjectId> {
    //fun findRandomDelivery(): Delivery
    fun findByOrderID(id: String): List<Optional<Delivery>>
}