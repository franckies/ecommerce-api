package it.polito.master.ap.group6.ecommerce.orderservice.services

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.MailingInfoDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.RollbackDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.MicroService
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface OrderServiceAsync {
    fun createOrder(placedOrder: PlacedOrderDTO): Response
    fun productsChecked(deliveryList: DeliveryListDTO): Response
    fun walletChecked(orderId: String): Response
    fun saveDeliveries(deliveryList: DeliveryListDTO, address: String): Unit
    fun cancelOrder(orderId: ObjectId): Response
    fun rollbackOrder(orderId: String): Response
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
        //STEP 1: check if pending is already logged
        val orderLoggerOptional = orderLoggerRepository.findByOrderIDAndOrderStatus(
            placedOrder.sagaID.toString(),
            OrderLoggerStatus.PENDING
        )
        if (orderLoggerOptional.isPresent) {
            println("OrderServiceAsync.createOrder: skipped duplicate order ${placedOrder.sagaID.toString()}")
            return Response.invalidOrder()
        }
        //check if the order is already failed (it happens when product ok or wallet ok comes first and then the order is rollbacked)
        if (checkRollbackCondition(placedOrder.sagaID.toString())) {
            println("OrderServiceAsync.createOrder: skipped duplicate order ${placedOrder.sagaID.toString()}")
            return Response.invalidOrder()
        }

        //STEP 2: log the created order and save it into the DB with pending status
        val order: Order = placedOrder.toModel()
        order.status = OrderStatus.PENDING
        orderRepository.save(order)
        orderLoggerRepository.save(OrderLogger(placedOrder.sagaID, OrderLoggerStatus.PENDING))

        //STEP 3: set a timer and either create or rollback the order
        val performOrder = GlobalScope.launch() {
            delay(2000L)
            if (checkConditions(placedOrder.sagaID.toString())) {
                println("OrderServiceAsync.createOrder.performOrder: Timer expired. The order is confirmed.")
                order.status = OrderStatus.PAID
                orderRepository.save(order)
                sendEmail(order.id.toString(), "The order has been confirmed!")
                //start deliveries
                deliveryService.startDeliveries(order.id.toString())
                //return Response.orderConfirmed()
            } else {
                println("OrderServiceAsync.createOrder.performOrder: Timer expired. The order is failed and a rollback is requested.")
                kafkaTemplate.send(
                    "rollback",
                    Gson().toJson(RollbackDTO(placedOrder.sagaID.toString(), MicroService.ORDER_SERVICE)).toString()
                )
            }
            this.cancel()
        }
        //Wait for the other responses.
        return Response.waiting()
    }

    override fun productsChecked(deliveryList: DeliveryListDTO): Response {
        //sleep(500L)
        //STEP 1: check if delivery ok is already logged
        val orderLoggerOptional = orderLoggerRepository.findByOrderIDAndOrderStatus(
            deliveryList.orderID.toString(),
            OrderLoggerStatus.DELIVERY_OK
        )
        if (orderLoggerOptional.isPresent) {
            println("OrderServiceAsync.productsChecked: skipped duplicate order ${deliveryList.orderID.toString()}")
            return Response.invalidOrder()
        }
        //STEP 2: log the created order
        orderLoggerRepository.save(OrderLogger(deliveryList.orderID, OrderLoggerStatus.DELIVERY_OK))
        //save the received deliveries
        saveDeliveries(deliveryList, deliveryList.deliveryAddress!!)

        //If the create order is not received within a given timer, then rollback the order.
        val creationTimer = GlobalScope.launch {
            delay(10_000L)
            val orderLoggerOptional =
                orderLoggerRepository.findByOrderIDAndOrderStatus(
                    deliveryList.orderID.toString(),
                    OrderLoggerStatus.PENDING
                )
            if (orderLoggerOptional.isEmpty) {
                println("OrderServiceAsync.productsChecked.creationTimer: Timer expired. The order is failed and a rollback is requested.")
                kafkaTemplate.send(
                    "rollback",
                    Gson().toJson(RollbackDTO(deliveryList.orderID.toString(), MicroService.ORDER_SERVICE)).toString()
                )
            }
            this.cancel()
        }

        //Wait for the other responses.
        return Response.waiting()
    }

    override fun walletChecked(orderId: String): Response {
        //sleep(1000L)
        //STEP 1: check if transaction ok is already logged
        val orderLoggerOptional = orderLoggerRepository.findByOrderIDAndOrderStatus(
            orderId,
            OrderLoggerStatus.TRANSACTION_OK
        )
        if (orderLoggerOptional.isPresent) {
            println("OrderServiceAsync.walletChecked: skipped duplicate order ${orderId}")
            return Response.invalidOrder()
        }
        //STEP 2: log the created order
        orderLoggerRepository.save(OrderLogger(orderId, OrderLoggerStatus.TRANSACTION_OK))

        //If the create order is not received within a given timer, then rollback the order.
        val creationTimer = GlobalScope.launch {
            delay(10_000L)
            val orderLoggerOptional =
                orderLoggerRepository.findByOrderIDAndOrderStatus(orderId.toString(), OrderLoggerStatus.PENDING)
            if (orderLoggerOptional.isEmpty) {
                println("OrderServiceAsync.walletChecked.creationTimer: Timer expired. The order is failed and a rollback is requested.")
                kafkaTemplate.send(
                    "rollback",
                    Gson().toJson(RollbackDTO(orderId.toString(), MicroService.ORDER_SERVICE)).toString()
                )
            }
            this.cancel()
        }
        //Wait for the other responses.
        return Response.waiting()
    }

    override fun saveDeliveries(deliveryList: DeliveryListDTO, address: String) {
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

    override fun cancelOrder(orderId: ObjectId): Response {
        //check if is already failed or canceled
        if (checkRollbackCondition(orderId.toString())) {
            return Response.invalidOrder()
        }

        //check if it is an existing order
        val orderOptional = orderRepository.findById(orderId)
        if (orderOptional.isEmpty) {
            println("OrderServiceAsync.cancelOrder: The order $orderId cannot be found.")
            return Response.orderCannotBeFound()
        }
        //check if can be canceled (i.e. is in PAID status)
        val order = orderOptional.get()
        if (order.status == OrderStatus.PAID) {
            //update the order
            order.status = OrderStatus.CANCELED
            orderRepository.save(order)
            //update the logger
            orderLoggerRepository.save(OrderLogger(orderId.toString(), OrderLoggerStatus.FAILED))
            //publish on kafka
            println("OrderService.cancelOrder: published on topic cancel_order with message $orderId .")
            kafkaTemplate.send("cancel_order", orderId.toString())
            sendEmail(orderId.toString(), "The order has been successfully canceled!")
            println("OrderServiceAsync.cancelOrder: Order ${order.id} canceled!")
        } else {
            //sendEmail(orderId.toString(), "The order cannot be canceled!")
            println("OrderServiceAsync.cancelOrder: Cannot cancel the order ${order.id}!")
        }
        val res = Response.orderFound()
        res.body = order.toDto()
        return res
    }

    /**
     * Performs the rollback of an order updating both the logger and the db.
     * @return Response.invalidOrder() if the order cannot be rollbacked (it is a duplicate), Response.orderRollback() otherwise.
     */
    override fun rollbackOrder(orderId: String): Response {
        //skip duplicate rollback requests
        if (checkRollbackCondition(orderId)) {
            return Response.invalidOrder()
        }
        //fail the order in the logger and in the db
        orderLoggerRepository.save(OrderLogger(orderId, OrderLoggerStatus.FAILED))
        val orderOptional = orderRepository.findById(ObjectId(orderId))
        if (orderOptional.isEmpty) {
            return Response.invalidOrder()
        }
        val order = orderOptional.get()
        order.status = OrderStatus.FAILED
        orderRepository.save(order)
        sendEmail(orderId.toString(), "An error occurred processing the order. Retry!")
        return Response.orderRollback()
    }

    /**
     * Publish on topic order_tracking to inform MailingService to send an email to a customer.
     */
    override fun sendEmail(orderId: String, message: String) {
        val orderOptional = orderRepository.findById(ObjectId(orderId))
        if (orderOptional.isEmpty) {
            return
        }
        val order = orderOptional.get()
        println("OrderService.sendEmail: Published on topic order_tracking with message $message.")
        kafkaTemplate.send(
            "order_tracking",
            Gson().toJson(MailingInfoDTO(order.buyerId, order.status, order.id, message, null, null)).toString()
        )
    }

    /**
     * @return true if the order can be set as PAID, false otherwise.
     */
    fun checkConditions(orderID: String): Boolean {
        val orderLoggerListOptional = orderLoggerRepository.findByOrderID(orderID)
        if (orderLoggerListOptional.isEmpty) {
            return false
        }
        val orderLoggerList = orderLoggerListOptional.get()
        val orderLoggerStatusList = orderLoggerList.map { it.orderStatus }
        if (OrderLoggerStatus.FAILED !in orderLoggerStatusList) {
            return (OrderLoggerStatus.DELIVERY_OK in orderLoggerStatusList
                    && OrderLoggerStatus.TRANSACTION_OK in orderLoggerStatusList
                    && OrderLoggerStatus.PENDING in orderLoggerStatusList)
        }
        return false
    }

    /**
     * @return true if the order has already been rollbacked, false otherwise.
     */
    fun checkRollbackCondition(orderID: String): Boolean {
        val orderLoggerListOptional = orderLoggerRepository.findByOrderID(orderID)
        if (orderLoggerListOptional.isEmpty) {
            return false
        }
        val orderLoggerList = orderLoggerListOptional.get()

        val orderLoggerStatusList = orderLoggerList.map { it.orderStatus }

        return OrderLoggerStatus.FAILED in orderLoggerStatusList
    }
}