package it.polito.master.ap.group6.ecommerce.orderservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.util.*

interface OrderService {
    fun createOrder(placedOrder: PlacedOrderDTO): Order
    fun getOrder(orderID: ObjectId): Optional<Order>
    fun getOrdersByUser(userID: String): List<Optional<Order>>
    fun cancelOrder(orderID: ObjectId): Optional<Order>
}

/**
 * The order service. Implements the business logic.
 * @param orderRepository a reference to the Repository handling the database interaction.
 * @author Francesco Semeraro
 */
@Service
class OrderServiceImpl(private val orderRepository: OrderRepository) : OrderService {

    //TODO: user's wallet must be checked before creating an order.
    override fun createOrder(placedOrder: PlacedOrderDTO): Order =
        orderRepository.save(placedOrder.toModel())

    override fun getOrder(orderID: ObjectId): Optional<Order> =
        orderRepository.findById(orderID)

    override fun getOrdersByUser(userID: String): List<Optional<Order>> =
        orderRepository.findByBuyerId(userID)

    override fun cancelOrder(orderID: ObjectId): Optional<Order> {
        val order = orderRepository.findById(orderID)
        if (order.get().status == OrderStatus.PAID) {
            order.get().status = OrderStatus.CANCELLED
            orderRepository.save(order.get())
            println("OrderService: Order canceled!")
        } else {
            println("OrderService: Cannot cancel this order!") //TODO: how to return an error?
        }
        return order
    }
}