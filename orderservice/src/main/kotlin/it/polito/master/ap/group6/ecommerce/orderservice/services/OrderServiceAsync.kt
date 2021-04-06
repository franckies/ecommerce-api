package it.polito.master.ap.group6.ecommerce.orderservice.services

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.MailingInfoDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.OrderLogger
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderLoggerRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface OrderServiceAsync {
    fun createOrder(placedOrder: PlacedOrderDTO): Response
    fun productsChecked(deliveryList: DeliveryListDTO): Response
    fun walletChecked(orderId: String): Response
    fun saveDeliveries(deliveryList: DeliveryListDTO, address: String): Unit
    fun cancelOrder(orderId: ObjectId): Response
    fun rollbackOrder(orderId: String): Response
    fun failOrder(orderId: String)
    fun sendEmail(orderId: String, message: String)
}

/**
 * The order service. Implements the business logic.
 * @param orderRepository a reference to the Repository handling the database interaction for orders.
 * @param deliveryRepository a reference to the Repository handling the database interaction for deliveries.
 * @param deliveryService the service simulating the shipping of the orders.
 * @param kafkaTemplate  a reference to the kafkaTemplate to publish rollback information
 * @author Francesco Semeraro
 */
@Service
@Transactional
class OrderServiceAsyncImpl(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val deliveryService: DeliveryService,
    @Autowired private val orderLoggerRepository: OrderLoggerRepository,
    val kafkaTemplate: KafkaTemplate<String, String>,
) : OrderServiceAsync {
    override fun createOrder(placedOrder: PlacedOrderDTO): Response {
        //check if the order is already present, if it is, something went wrong.
        //fail the order and rollback all
        if (orderRepository.findById(ObjectId(placedOrder.sagaID)).isPresent) {
            //delete the order from the logger
            orderLoggerRepository.deleteById(ObjectId(placedOrder.sagaID))
            //fail the order in the database
            failOrder(placedOrder.sagaID.toString())
            return Response.invalidOrder()
        }
        //save the order in the database and in the logger
        val order: Order = placedOrder.toModel()
        order.status = OrderStatus.PENDING
        orderRepository.save(order)

        orderLoggerRepository.save(OrderLogger(placedOrder.sagaID, OrderLoggerStatus.PENDING, Date()))

        //timer for the other services to answer
        GlobalScope.launch() {
            delay(120_000L)
            val orderLogger = orderLoggerRepository.findById(ObjectId(order.id))
            if (orderLogger.isPresent) {
                when (orderLogger.get().orderStatus) {
                    OrderLoggerStatus.PENDING -> {
                        println("OrderService.timer: published on topic cancel_order.")
                        kafkaTemplate.send("rollback", orderLogger.get().orderID)
                    }
                    OrderLoggerStatus.DELIVERY_OK -> {
                        println("OrderService.timer: published on topic cancel_order.")
                        kafkaTemplate.send("rollback", orderLogger.get().orderID)
                    }

                    OrderLoggerStatus.TRANSACTION_OK -> {
                        println("OrderService.timer: published on topic cancel_order.")
                        kafkaTemplate.send("rollback", orderLogger.get().orderID)
                    }
                    else -> println("OrderServiceAsync.createOrder: timer expired and all is ok.")
                }
            }

        }

        return Response.orderCreated()
    }

    override fun productsChecked(deliveryList: DeliveryListDTO): Response {
        val orderId: ObjectId = ObjectId(deliveryList.orderID)
        val orderLoggerOptional = orderLoggerRepository.findById(orderId)
        //If the order is not logget, something went wrong. Rollback all
        if (orderLoggerOptional.isEmpty) {
            failOrder(orderId.toString())
            return Response.invalidOrder()
        }
        when (orderLoggerOptional.get().orderStatus) {
            //If the order is pending, then the warehouse is the first answering, and the deliveries are saved.
            OrderLoggerStatus.PENDING -> {
                orderLoggerRepository.save(OrderLogger(orderId.toString(), OrderLoggerStatus.DELIVERY_OK, Date()))
                //save created deliveries in the db
                saveDeliveries(deliveryList, orderRepository.findById(orderId).get().deliveryAddress!!)
                return Response.orderSubmitted()
            }
            //If the transaction is completed, then the wallet already answered, the order is completed
            OrderLoggerStatus.TRANSACTION_OK -> {
                //log the order as paid
                orderLoggerRepository.save(OrderLogger(orderId.toString(), OrderLoggerStatus.PAID, Date()))
                //save the order as paid
                var order = orderRepository.findById(orderId).get()
                order.status = OrderStatus.PAID
                order = orderRepository.save(order)
                //create and start deliveries
                saveDeliveries(deliveryList, order.deliveryAddress!!)
                deliveryService.startDeliveries(order.id.toString())
                return Response.orderConfirmed()
            }
            //rollback all if none of the previous status is observed. Something went wrong.
            else -> {
                //delete the order from the logger
                orderLoggerRepository.deleteById(orderId)
                //fail the order in the database
                failOrder(orderId.toString())
                return Response.invalidOrder()
            }
        }
    }

    override fun walletChecked(orderId: String): Response {
        val orderLoggerOptional = orderLoggerRepository.findById(ObjectId(orderId))
        //If the order is not logged, something went wrong. Rollback all
        if (orderLoggerOptional.isEmpty) {
            failOrder(orderId)
            return Response.invalidOrder()
        }
        when (orderLoggerOptional.get().orderStatus) {
            //If the order is pending, then the wallet is the first answering, and the payment is completed.
            OrderLoggerStatus.PENDING -> {
                orderLoggerRepository.save(OrderLogger(orderId, OrderLoggerStatus.TRANSACTION_OK, Date()))
                return Response.moneyLocked()
            }
            //If the order is submitted, then the warehouse already answered, the order is completed
            OrderLoggerStatus.DELIVERY_OK -> {
                //log the order as paid
                orderLoggerRepository.save(OrderLogger(orderId, OrderLoggerStatus.PAID, Date()))
                //save the order as paid
                var order = orderRepository.findById(ObjectId(orderId)).get()
                order.status = OrderStatus.PAID
                order = orderRepository.save(order)
                //start deliveries
                deliveryService.startDeliveries(order.id.toString())
                return Response.orderConfirmed()
            }
            //rollback all if none of the previous status is observed. Something went wrong.
            else -> {
                //delete the order from the logger
                orderLoggerRepository.deleteById(ObjectId(orderId))
                //fail the order in the database
                failOrder(orderId)
                return Response.invalidOrder()
            }
        }
    }

    override fun cancelOrder(orderId: ObjectId): Response {
        val orderOptional = orderRepository.findById(orderId)
        if (orderOptional.isEmpty) {
            println("OrderServiceAsync.cancelOrder: The order $orderId cannot be found.")
            return Response.orderCannotBeFound()
        }
        val order = orderOptional.get()
        if (order.status == OrderStatus.PAID) {
            order.status = OrderStatus.CANCELED
            orderRepository.save(order)
            //delete the logs for that order
            orderLoggerRepository.deleteById(orderId)
            println("OrderService.cancelOrder: published on topic cancel_order with message $orderId .")
            kafkaTemplate.send("cancel_order", orderId.toString())
            sendEmail(orderId.toString(), "The order has been successfully canceled!")
            println("OrderServiceAsync.cancelOrder: Order ${order.id} canceled!")
        } else {
            sendEmail(orderId.toString(), "The order cannot be canceled!")
            println("OrderServiceAsync.cancelOrder: Cannot cancel the order ${order.id}!")
        }
        val res = Response.orderFound()
        res.body = order.toDto()
        return res
    }

    override fun rollbackOrder(orderId: String): Response {
        val orderLoggerOptional = orderLoggerRepository.findById(ObjectId(orderId))
        //If the order is not logged, something went wrong. Rollback all
        if (orderLoggerOptional.isEmpty) {
            //set order as failed
            failOrder(orderId)
            return Response.invalidOrder()
        }

        //If the order is logged, then it must be in one among delivery ok or transaction ok or pending
        //Then or products are not available or there aren't enough money
        when (orderLoggerOptional.get().orderStatus) {
            OrderLoggerStatus.DELIVERY_OK -> {
                //unlog the order
                orderLoggerRepository.deleteById(ObjectId(orderId))
                //set it as failed
                failOrder(orderId)
                return Response.notEnoughMoney()
            }
            OrderLoggerStatus.TRANSACTION_OK -> {
                //unlog the order
                orderLoggerRepository.deleteById(ObjectId(orderId))
                //set it as failed
                failOrder(orderId)
                return Response.productNotAvailable()
            }
            OrderLoggerStatus.PENDING -> {
                //unlog the order
                orderLoggerRepository.deleteById(ObjectId(orderId))
                //set it as failed
                failOrder(orderId)
                return Response.invalidOrder()
            }
            else -> {
                return Response.invalidOrder()
            }
        }
    }

    override fun saveDeliveries(deliveryList: DeliveryListDTO, address: String): Unit {
        for (delivery in deliveryList.deliveryList!!) {
            //save each delivery in the database with a PENDING status
            deliveryRepository.save(
                Delivery(
                    deliveryList.orderID,
                    address,
                    delivery.warehouseID,
                    delivery.purchases?.map { it.toModel() },
                    DeliveryStatus.PENDING
                )
            )
        }
    }

    override fun failOrder(orderId: String): Unit {
        val failedOrderOptional = orderRepository.findById(ObjectId(orderId))
        if (failedOrderOptional.isPresent) {
            failedOrderOptional.get().status = OrderStatus.FAILED
            orderRepository.save(failedOrderOptional.get())
        }
    }

    override fun sendEmail(orderId: String, message: String): Unit {

        val order = orderRepository.findById(ObjectId(orderId)).get()
        println("OrderService.sendEmail: Published on topic order_tracking with message.")
        kafkaTemplate.send(
            "order_tracking",
            Gson().toJson(MailingInfoDTO(order.buyerId, order.status, order.id, message)).toString()
        )
    }
}