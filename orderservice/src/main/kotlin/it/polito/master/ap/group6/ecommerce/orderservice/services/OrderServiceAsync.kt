package it.polito.master.ap.group6.ecommerce.orderservice.services

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
 * @param kafkaTemplateRollback  a reference to the kafkaTemplate to publish rollback information
 * @param kafkaTemplateMailing a reference to the kafkaTemplate to publish a new mail to be sent
 * @author Francesco Semeraro
 */
@Service
@Transactional
class OrderServiceAsyncImpl(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val deliveryService: DeliveryService,
    @Autowired private val orderLoggerRepository: OrderLoggerRepository,
    val kafkaTemplateRollback: KafkaTemplate<String, String>,
    val kafkaTemplateMailing: KafkaTemplate<String, MailingInfoDTO>
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
        return Response.orderCreated()
    }

    override fun productsChecked(deliveryList: DeliveryListDTO): Response {
        val orderId: ObjectId = ObjectId(deliveryList.orderID)
        val orderLoggerOptional = orderLoggerRepository.findById(orderId)
        //If the order is not logget, something went wrong. Rollback all
        if(orderLoggerOptional.isEmpty){
            failOrder(orderId.toString())
            return Response.invalidOrder()
        }
        //If the order is not logged as pending or paid, something went wrong. Rollback all
        if (orderLoggerOptional.get().orderStatus != OrderLoggerStatus.PENDING || orderLoggerOptional.get().orderStatus != OrderLoggerStatus.COMPLETE_TRANSACTION_COMPLETED) {
            //delete the order from the logger
            orderLoggerRepository.deleteById(orderId)
            //fail the order in the database
            failOrder(orderId.toString())
            return Response.invalidOrder()
        }
        //If the order is pending, then the warehouse is the first answering, and the submit is completed.
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.PENDING){

            orderLoggerRepository.save(OrderLogger(orderId.toString(), OrderLoggerStatus.SUBMIT_ORDER_COMPLETED, Date()))
            //save created deliveries in the db
            saveDeliveries(deliveryList, orderRepository.findById(orderId).get().deliveryAddress!!)
            return Response.orderSubmitted()
        }
        //If the order is paid, then the wallet has already answered, the order is then completed.
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.COMPLETE_TRANSACTION_COMPLETED) {
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
        return Response.invalidOrder()
    }

    override fun walletChecked(orderId: String): Response {
        val orderLoggerOptional = orderLoggerRepository.findById(ObjectId(orderId))
        //If the order is not logged, something went wrong. Rollback all
        if(orderLoggerOptional.isEmpty){
            failOrder(orderId)
            return Response.invalidOrder()
        }
        //If the order is not logged as pending or submitted, something went wrong. Rollback all
        if (orderLoggerOptional.get().orderStatus != OrderLoggerStatus.PENDING || orderLoggerOptional.get().orderStatus != OrderLoggerStatus.SUBMIT_ORDER_COMPLETED) {
            //delete the order from the logger
            orderLoggerRepository.deleteById(ObjectId(orderId))
            //fail the order in the database
            failOrder(orderId)
            return Response.invalidOrder()
        }
        //If the order is pending, then the wallet is the first answering, and the payment is completed.
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.PENDING){
            orderLoggerRepository.save(OrderLogger(orderId, OrderLoggerStatus.COMPLETE_TRANSACTION_COMPLETED, Date()))
            return Response.moneyLocked()
        }
        //If the order is submitted, then the warehouse already answered, the order is completed
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.SUBMIT_ORDER_COMPLETED) {
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
        return Response.invalidOrder()


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
            kafkaTemplateRollback.send("rollback", orderId.toString())
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
        if(orderLoggerOptional.isEmpty){
            //set order as failed
            failOrder(orderId)
            return Response.invalidOrder()
        }
        //If the order is logged, then it must be in one among submit completed or transaction completed or pending
        //Then or products are not available or there aren't enough money
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.SUBMIT_ORDER_COMPLETED){
            //unlog the order
            orderLoggerRepository.deleteById(ObjectId(orderId))
            //set it as failed
            failOrder(orderId)
            return Response.notEnoughMoney()
        }
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.COMPLETE_TRANSACTION_COMPLETED){
            //unlog the order
            orderLoggerRepository.deleteById(ObjectId(orderId))
            //set it as failed
            failOrder(orderId)
            return Response.productNotAvailable()
        }
        if(orderLoggerOptional.get().orderStatus == OrderLoggerStatus.PENDING){
            //unlog the order
            orderLoggerRepository.deleteById(ObjectId(orderId))
            //set it as failed
            failOrder(orderId)
            return Response.invalidOrder()
        }
        return Response.invalidOrder()
    }

    override fun saveDeliveries(deliveryList: DeliveryListDTO, address: String): Unit{
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

    override fun failOrder(orderId: String): Unit{
        val failedOrderOptional = orderRepository.findById(ObjectId(orderId))
        if(failedOrderOptional.isPresent){
            failedOrderOptional.get().status = OrderStatus.FAILED
            orderRepository.save(failedOrderOptional.get())
        }
    }

    override  fun sendEmail(orderId: String, message: String): Unit{
        val order = orderRepository.findById(ObjectId(orderId)).get()
        kafkaTemplateMailing.send("order_tracking", MailingInfoDTO(order.buyerId, order.status, order.id, message))
    }
}