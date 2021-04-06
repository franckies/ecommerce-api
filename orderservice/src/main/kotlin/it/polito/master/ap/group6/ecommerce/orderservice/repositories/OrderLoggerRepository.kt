package it.polito.master.ap.group6.ecommerce.orderservice.repositories

import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.OrderLogger
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderLoggerRepository : MongoRepository<OrderLogger, ObjectId> {
    fun findByOrderIDAndOrderStatus(OrderID: String, OrderStatus: OrderLoggerStatus): Optional<OrderLogger>

    fun findByOrderID(OrderID: String): Optional<List<OrderLogger>>


}