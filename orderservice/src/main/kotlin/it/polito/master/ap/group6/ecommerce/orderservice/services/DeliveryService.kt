package it.polito.master.ap.group6.ecommerce.orderservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.OrderLogger
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderLoggerRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.*

/**
 * The delivery service. Handles the simulation of the delivery mechanism, scheduling the deliveries and updating
 * the delivery status and the order status consequently.
 * @param orderRepository a reference to the Repository handling the database interaction for orders.
 * @param deliveryRepository a reference to the Repository handling the database interaction for deliveries.
 * @author Francesco Semeraro
 */
@Service
@Transactional
class DeliveryService(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val orderLoggerRepository: OrderLoggerRepository
) {
    fun startDeliveries(orderID: String): Unit {
        //After a while update randomly a delivery status for testing
        val purchaseJob = GlobalScope.launch { // launch a new coroutine in background and continue
            while (true) {
                delay(120000L)

                val orderOptional = orderRepository.findById(ObjectId(orderID))
                val order: Order
                if (orderOptional.isEmpty) break
                else order = orderOptional.get()

                val deliveries = deliveryRepository.findByOrderID(order.id!!)

                //CASE 0: The order is in FAILED status -> something went wrong, the delivery coroutine is called after
                //setting the order to PAID status.
                if (order.status == OrderStatus.FAILED ){
                    println("DeliveryService.startDeliveries: Something went wrong with the order $orderID")
                    break
                }

                //CASE 1: The order has been CANCELED -> CANCEL all the associated deliveries.
                if (order.status == OrderStatus.CANCELED) {
                    deliveries.all { it.get().status == DeliveryStatus.CANCELED }
                    println("DeliveryService.startDeliveries: The order has been canceled or is in the status failed .")
                    break
                }

                //CASE 2: the order is still PENDING -> there are no associated deliveries.
                if (order.status == OrderStatus.PENDING) {
                    println("DeliveryService.startDeliveries: The order is still in the status pending.")
                    continue
                }

                //CASE3: all the deliveries of this order have been delivered.
                if (deliveries.all { it.get().status == DeliveryStatus.DELIVERING }) {
                    println("DeliveryService.startDeliveries: All deliveries associated to ${order.id} have been shipped.")
                    break
                }

                //CASE4: pick random a delivery and ship it, updating the order status.
                val randomDelivery = deliveries.get(Random().nextInt(deliveries.size))
                if (randomDelivery.get().status == DeliveryStatus.PENDING) {
                    randomDelivery.get().status = DeliveryStatus.DELIVERING
                    deliveryRepository.save(randomDelivery.get())
                    //CONSEQUENTLY UPDATE THE ORDER
                    if (order.status == OrderStatus.PAID) {
                        order.status = OrderStatus.DELIVERING
                        orderRepository.save(order)
                        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.DELIVERING, Date()))
                    }
                    println("DeliveryService.startDeliveries: The delivery ${randomDelivery.get().id} associated to the order ${order.id} has been shipped.")
                }
            }
            println("DeliveryService.startDeliveries: Shipping coroutine for the order $orderID exited with success.")
            this.cancel()
        }
    }
}