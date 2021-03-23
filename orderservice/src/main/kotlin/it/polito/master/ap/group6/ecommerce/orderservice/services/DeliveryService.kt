package it.polito.master.ap.group6.ecommerce.orderservice.services

import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    @Autowired private val deliveryRepository: DeliveryRepository
) {
    fun startDeliveries(orderID: String): Unit {
        //After a while update randomly a delivery status for testing
        val purchaseJob = GlobalScope.launch { // launch a new coroutine in background and continue
            while (true) {
                delay(10000L)

                val orderOptional = orderRepository.findById(ObjectId(orderID))
                val order: Order
                if (orderOptional.isEmpty) return@launch
                else order = orderOptional.get()

                val deliveries = deliveryRepository.findByOrderID(order.id!!)
                if (order.status == OrderStatus.FAILED || order.status == OrderStatus.CANCELED) {
                    deliveries.all { it.get().status == DeliveryStatus.CANCELED }
                    println("DeliveryService.startDeliveries: The order has been canceled or is in the status failed .")
                    break //exit if the order failed in the STEP 3 (or if it has been canceled) and set all scheduled delivery to canceled.
                }
                if (order.status == OrderStatus.PENDING) {
                    println("DeliveryService.startDeliveries: The order is still in the status pending.")
                    continue //continue if the order is not yet confirmed, i.e. PAID
                }
                if (deliveries.all { it.get().status == DeliveryStatus.DELIVERING }) {
                    println("DeliveryService.startDeliveries: All deliveries associated to ${order.id} have been shipped.")
                    break
                }
                val randomDelivery = deliveries.get(Random().nextInt(deliveries.size))
                if (randomDelivery.get().status == DeliveryStatus.PENDING) {
                    randomDelivery.get().status = DeliveryStatus.DELIVERING
                    deliveryRepository.save(randomDelivery.get())
                    //CONSEQUENTLY UPDATE THE ORDER
                    if (order.status == OrderStatus.PAID) {
                        order.status = OrderStatus.DELIVERING
                        orderRepository.save(order)
                    }
                    println("DeliveryService.startDeliveries: The delivery ${randomDelivery.get().id} associated to the order ${order.id} has been shipped.")
                }
            }
            println("DeliveryService.startDeliveries: Shipping coroutine for the order $orderID exited with success.")
            this.cancel()
        }
    }
}