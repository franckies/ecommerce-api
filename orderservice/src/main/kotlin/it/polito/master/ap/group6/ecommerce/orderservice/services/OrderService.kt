package it.polito.master.ap.group6.ecommerce.orderservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
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
class OrderServiceImpl(
    @Autowired private val orderRepository: OrderRepository
) : OrderService {

    //TODO: user's wallet must be checked before creating an order, thus the status must be set.
    override fun createOrder(placedOrder: PlacedOrderDTO): Order {
        var wallet: String = "localhost:8083"
        val restTemplate = RestTemplate()

        //How to do a GET on another service
        val userID = placedOrder.user?.id
        //val walletDTO = restTemplate.getForObject("http://${wallet}/wallet/${userID}", WalletDTO::class.java)

        //How to do a POST on another service
        val transaction = TransactionDTO(placedOrder.user, placedOrder.toModel().price, null, null, null)
        restTemplate.postForObject<TransactionDTO>( "http://${wallet}/wallet/${userID}",
            transaction, Boolean::class.java)
        return orderRepository.save(placedOrder.toModel())
    }

    override fun getOrder(orderID: ObjectId): Optional<Order> =
        orderRepository.findById(orderID)

    override fun getOrdersByUser(userID: String): List<Optional<Order>> =
        orderRepository.findByBuyerId(userID)

    override fun cancelOrder(orderID: ObjectId): Optional<Order> {
        val order = orderRepository.findById(orderID)
        if (order.isEmpty) {
            return order
        }
        if (order.get().status == OrderStatus.PAID) {
            //TODO: Check Update method class Update().set("status", OrderStatus.CANCELLED)
            order.get().status = OrderStatus.CANCELLED
            orderRepository.save(order.get())
            println("OrderService: Order ${order.get().id} canceled!")
        } else {
            println("OrderService: Cannot cancel the order ${order.get().id}!") //TODO: how to return an error?
        }
        return order
    }
}