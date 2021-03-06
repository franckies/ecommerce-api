package it.polito.master.ap.group6.ecommerce.orderservice.listeners


import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.RollbackDTO
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.orderservice.services.OrderServiceAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.util.*

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
                println("OrderSagaListener.createOrder: skipping duplicated saga ID ${placedOrder.sagaID.toString()}")
            }
            ResponseType.WAITING -> {
                println("OrderSaga.createOrder: Waiting for other MS to respond for order ${placedOrder.sagaID}.")
            }
            else -> println("OrderSaga.createOrder: $response")
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["products_ok"])
    fun productsChecked(deliveryListSer: String) {
        println("OrderSagaListener.productsChecked: Received Kafka message on topic products_ok with message $deliveryListSer")
        val deliveryList: DeliveryListDTO = json.fromJson(deliveryListSer, DeliveryListDTO::class.java)
        val response: Response = orderServiceAsync.productsChecked(deliveryList)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                println("OrderSagaListener.productsChecked: skipping duplicated saga ID ${deliveryList.orderID}")
            }
            ResponseType.WAITING -> {
                println("OrderSaga.productsChecked: Waiting for other MS to respond for order ${deliveryList.orderID}.")
            }
            else -> println("OrderSaga.productsChecked: $response")
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["wallet_ok"])
    fun walletChecked(orderId: String) {
        println("OrderSagaListener.walletChecked: Received Kafka message on topic wallet_ok with message $orderId")
        val response: Response = orderServiceAsync.walletChecked(orderId)
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                println("OrderSagaListener.walletChecked: skipping duplicated saga ID ${orderId}")
            }
            ResponseType.WAITING -> {
                println("OrderSaga.walletChecked: Waiting for other MS to respond for order ${orderId}.")
            }
            else -> println("OrderSaga.walletChecked: $response")
        }
    }

    @KafkaListener(groupId = "orderservice", topics = ["rollback"])
    fun rollbackOrder(rollbackDTOSer: String) {
        println("OrderSagaListener.rollbackOrder: Received Kafka message on topic rollback with message $rollbackDTOSer")
        val rollbackDTO: RollbackDTO = json.fromJson(rollbackDTOSer, RollbackDTO::class.java)
        val response: Response = orderServiceAsync.rollbackOrder(rollbackDTO.sagaID.toString())
        when (response.responseId) {
            ResponseType.INVALID_ORDER -> {
                println("OrderSagaListener.rollbackOrder: skipping duplicated saga ID ${rollbackDTO.sagaID.toString()}")
            }
            ResponseType.ROLLBACK_OK -> {
                println("OrderSaga.rollbackOrder: The order ${rollbackDTO.sagaID.toString()} has been canceled due to errors.")
            }
            else -> println("OrderSaga.rollbackOrder: $response")
        }
    }
}