package it.polito.master.ap.group6.ecommerce.walletservice.listeners

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.RollbackDTO
import it.polito.master.ap.group6.ecommerce.common.misc.MicroService
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletServiceAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionStatus

@Service
class WalletSagaListener(
        val kafkaTemplate: KafkaTemplate<String, String>,
        @Autowired private val walletServiceAsync: WalletServiceAsync,
        @Autowired private val walletService: WalletService,
) {

    @KafkaListener(groupId = "walletservice", topics = ["create_order"])
    fun createTransaction(placedOrderString: String) {

        val placedOrder = jacksonObjectMapper().readValue<PlacedOrderDTO>(placedOrderString)

        println("WalletSagaListener.createTransaction: Received Kafka message on topic create_order with message $placedOrder")
        val response: Response = walletServiceAsync.createTransaction(placedOrder)
        if (response.responseId == ResponseType.USER_WALLET_TRANSACTION) {
            kafkaTemplate.send("wallet_ok", placedOrder.sagaID.toString())
        }
        else {
            val message: String? = jacksonObjectMapper().writeValueAsString(RollbackDTO(placedOrder.sagaID.toString(), MicroService.WALLET_SERVICE))
            kafkaTemplate.send("rollback", message)
            println("WalletSagaListener.createTransaction: An error occurred processing the order ${placedOrder.sagaID}. The order is failed.")
        }
    }

    @KafkaListener(groupId = "walletservice", topics = ["rollback"])
    fun rollbackTransaction(rollback: RollbackDTO) {
        println("WalletSagaListener.rollbackTransaction: Received Kafka message on topic rollback with message ${rollback.sagaID}, sent by ${rollback.sender}")
        val response: Response = walletService.undoTransaction(rollback.sagaID,
            it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus.REFUSED)

    }

    @KafkaListener(groupId = "walletservice", topics = ["cancel_order"])
    fun cancelTransaction(orderId: String) {
        println("WalletSagaListener.rollbackTransaction: Received Kafka message on topic cancel_order with message $orderId")
        val response: Response = walletService.undoTransaction(orderId,
            it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus.REFUNDED)

    }

}