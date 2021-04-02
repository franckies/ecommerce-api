package it.polito.master.ap.group6.ecommerce.walletservice.listeners

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletServiceAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class WalletSagaListener(
        val kafkaTemplate: KafkaTemplate<String, String>,
        @Autowired private val walletServiceAsync: WalletServiceAsync,
        @Autowired private val walletService: WalletService,
) {

    @KafkaListener(groupId = "ecommerce", topics = ["create_order"])
    fun createTransaction(placedOrderString: String) {

        val placedOrder = jacksonObjectMapper().readValue<PlacedOrderDTO>(placedOrderString)

        println("WalletSagaListener.createTransaction: Received Kafka message on topic create_order with message $placedOrder")
        val response: Response = walletServiceAsync.createTransaction(placedOrder)
        if (response.responseId == ResponseType.USER_WALLET_TRANSACTION) {
            kafkaTemplate.send("wallet_ok", placedOrder.sagaID.toString())
        }
        else {
            kafkaTemplate.send("rollback", placedOrder.sagaID.toString())
            println("WalletSagaListener.createTransaction: An error occurred processing the order ${placedOrder.sagaID}. The order is failed.")
        }
    }

    @KafkaListener(groupId = "ecommerce", topics = ["rollback"])
    fun rollbackTransaction(orderId: String) {
        println("WalletSagaListener.rollbackTransaction: Received Kafka message on topic rollback with message $orderId")
        val response: Response = walletService.undoTransaction(orderId)

    }
}