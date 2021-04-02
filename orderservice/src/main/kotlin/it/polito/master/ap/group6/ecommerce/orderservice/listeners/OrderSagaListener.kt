package it.polito.master.ap.group6.ecommerce.orderservice.listeners


import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.orderservice.services.OrderServiceAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/**
 * The order service. Implements the business logic.
 * @param kafkaTemplate a reference to kafka template handling publisher-subscriber paradigm.
 * @param orderServiceAsync a reference to the service handling the async order lifecycle.
 * @author Francesco Semeraro
 */
@Service
class OrderSagaListener(
    val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val orderServiceAsync: OrderServiceAsync,
) {
    private val json = Gson()
    @KafkaListener(groupId = "orderservice", topics = ["create_order"])
    fun createOrder(placedOrderSer: String) {
        println("OrderSagaListener.createOrder: Received Kafka message on topic create_order with message $placedOrderSer")
        val placedOrder: PlacedOrderDTO = json.fromJson(placedOrderSer, PlacedOrderDTO::class.java)
        val response: Response = orderServiceAsync.createOrder(placedOrder)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                kafkaTemplate.send("rollback", placedOrder.sagaID.toString())
                println("OrderSagaListener.createOrder: An error occurred processing the order ${placedOrder.sagaID}. The order is failed.")
                orderServiceAsync.sendEmail(
                    placedOrder.sagaID.toString(),
                    "An error occurred processing your order. Please retry."
                )
            }
            ResponseType.ORDER_CREATED -> {
                println("OrderSaga.createOrder: The order ${placedOrder.sagaID} has been created and is in the PENDING status.")
                orderServiceAsync.sendEmail(
                    placedOrder.sagaID.toString(),
                    "Your order has been created and we are processing it!"
                )
            }
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["products_ok"])
    fun productsChecked(deliveryListSer: String) {
        println("OrderSagaListener.productsChecked: Received Kafka message on topic products_ok with message $deliveryListSer")
        val deliveryList: DeliveryListDTO = json.fromJson(deliveryListSer, DeliveryListDTO::class.java)
        val response: Response = orderServiceAsync.productsChecked(deliveryList)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                kafkaTemplate.send("rollback", deliveryList.orderID.toString())
                println("OrderSaga.productsCheck: An error occurred processing the order ${deliveryList.orderID}. The order is failed.")
                orderServiceAsync.sendEmail(
                    deliveryList.orderID.toString(),
                    "An error occurred processing your order. Please retry."
                )
            }
            ResponseType.ORDER_SUBMITTED -> {
                println("OrderSagaListener.productsCheck: The products for the order ${deliveryList.orderID} are available. Waiting for wallet check.")
                orderServiceAsync.sendEmail(
                    deliveryList.orderID.toString(),
                    "The products are available. We are verifying your wallet..."
                )
            }
            ResponseType.ORDER_CONFIRMED -> {
                println("OrderSagaListener.productsCheck: The order ${deliveryList.orderID} has been confirmed and paid.")
                orderServiceAsync.sendEmail(
                    deliveryList.orderID.toString(),
                    "The order is confirmed and the deliveries have been scheduled!"
                )
            }
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["wallet_ok"])
    fun walletChecked(orderId: String) {
        println("OrderSagaListener.walletChecked: Received Kafka message on topic products_ok with message $orderId")
        val response: Response = orderServiceAsync.walletChecked(orderId)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                kafkaTemplate.send("rollback", orderId.toString())
                println("OrderSagaListener.walletChecked: An error occurred processing the order $orderId. The order is failed.")
                orderServiceAsync.sendEmail(orderId, "An error occurred processing your order. Please retry.")
            }
            ResponseType.MONEY_LOCKED -> {
                println("OrderSagaListener.walletChecked: The transaction for the order $orderId is confirmed. Waiting for products check.")
                orderServiceAsync.sendEmail(
                    orderId,
                    "Your wallet has been verified. We are checking if your products are still available..."
                )
            }
            ResponseType.ORDER_CONFIRMED -> {
                println("OrderSagaListener.walletChecked: The order $orderId has been confirmed and paid.")
                orderServiceAsync.sendEmail(orderId, "The order is confirmed and the deliveries have been scheduled!")
            }
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["rollback"])
    fun rollbackOrder(orderId: String) {
        println("OrderSagaListener.rollbackOrder: Received Kafka message on topic rollback with message $orderId")
        val response: Response = orderServiceAsync.rollbackOrder(orderId)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                println("OrderSagaListener.rollbackOrder: An error occurred processing the order $orderId. The order is failed.")
                orderServiceAsync.sendEmail(orderId, "An error occurred processing your order. Please retry.")
            }
            ResponseType.NO_PRODUCTS -> {
                println("OrderSagaListener.rollbackOrder: The order $orderId is failed since there aren't enough products.")
                orderServiceAsync.sendEmail(
                    orderId,
                    "The order has been canceled because there aren't enough products."
                )
            }
            ResponseType.NO_MONEY -> {
                println("OrderSagaListener.rollbackOrder: The order $orderId is failed since there aren't enough money.")
                orderServiceAsync.sendEmail(
                    orderId,
                    "The order has been canceled because there aren't enough money in your wallet."
                )
            }
        }
    }
}