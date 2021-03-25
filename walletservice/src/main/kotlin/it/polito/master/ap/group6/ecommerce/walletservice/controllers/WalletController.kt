package it.polito.master.ap.group6.ecommerce.walletservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * The wallet controller. Exposes the endpoints.
 * @param walletService a reference to the Service handling the business logic.
 * @author Andrea Biondo
 */
@RestController
@RequestMapping("/wallet")
class OrderController(
    @Autowired private val walletService: WalletService
) {
    /**
     * POST a transaction into the database.
     * Confirm or refuse a transaction according to products availability in the orderService
     * @return ID corresponding to the saved transaction.
     */
    @GetMapping("/performtransaction/{transactionID}")
    fun createTransaction(@PathVariable("transactionID") transactionID: String?): ResponseEntity<String?> {

        println("WalletController.createTransaction: transaction ${transactionID} is requested for confirm/delete")
        val transactionResult = walletService.createTransaction(transactionID)

        return when(transactionResult.responseId) {
            ResponseType.USER_WALLET_CONFIRM -> ResponseEntity(transactionResult.body as String, HttpStatus.OK)
            ResponseType.USER_WALLET_FAILED -> ResponseEntity(null, HttpStatus.NOT_FOUND)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }

    }

    /**
     * POST a transaction into the database.
     * Confirm or refuse a transaction according to products availability in the orderService
     * @return ID corresponding to the saved transaction.
     */
    @GetMapping("/undo/{orderID}")
    fun undoTransaction(@PathVariable("orderID") orderID: String?): ResponseEntity<String?> {

        println("WalletController.undoTransaction: transaction with order ${orderID} rollback has been issued")
        val transactionResult = walletService.undoTransaction(orderID)

        return when(transactionResult.responseId) {
            ResponseType.USER_WALLET_REFUND -> ResponseEntity(transactionResult.body as String, HttpStatus.OK)
            ResponseType.USER_WALLET_FAILED -> ResponseEntity(null, HttpStatus.NOT_FOUND)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }

    }

    /**
     * POST a transaction into the database.
     * Check if there are enough money in the userID wallet
     * @return ID corresponding to the saved transaction.
     */
    @PostMapping("/checkavailability/{userID}")
    fun checkTransaction(@RequestBody checkTransaction: TransactionDTO?, @PathVariable("userID") userID: String?): ResponseEntity<String?> {

        println("WalletController.checkTransaction: a transaction check has been issued to the user ${userID}")
        val transactionID = walletService.checkTransaction(checkTransaction,userID)

        return when(transactionID.responseId) {
            ResponseType.USER_WALLET_NOMONEY -> ResponseEntity(null, HttpStatus.CONFLICT)
            ResponseType.USER_WALLET_TRANSACTION -> ResponseEntity(transactionID.body as String, HttpStatus.OK)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }

    }

    /**
     * POST a transaction into the database.
     * Recharge a given amount to the userID wallet
     * @return ID corresponding to the saved transaction.
     */
    @PostMapping("/recharge/{userID}")
    fun createRecharge(@RequestBody placedRecharge: RechargeDTO, @PathVariable("userID") userID: String?): ResponseEntity<String?> {

        println("WalletController.createRecharge: a recharge has been issued to the user ${userID}")
        val transactionID = walletService.createRecharge(placedRecharge,userID)
        return when(transactionID.responseId) {
            ResponseType.USER_WALLET_RECHARGE -> ResponseEntity(transactionID.body as String, HttpStatus.OK)
            ResponseType.USER_WALLET_FAILED -> ResponseEntity(null, HttpStatus.NOT_FOUND)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }

    }

    /**
     * GET the wallet having orderID as identifier.
     * @return the DTO corresponding to the retrieved wallet.
     */
    @GetMapping("/{userID}")
    fun getWallet(@PathVariable("userID") userID: String?): ResponseEntity<Wallet?> {

        println("WalletController.getWallet: the user ${userID} wallet is requested")
        val wallet = walletService.getWallet(userID)
        return when(wallet.responseId) {
            ResponseType.USER_WALLET_GET -> ResponseEntity(wallet.body as Wallet, HttpStatus.OK)
            ResponseType.USER_WALLET_FAILED -> ResponseEntity(null, HttpStatus.NOT_FOUND)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }
    }

    /**
     * POST a new wallet into the database.
     * Create a wallet associated to the userDTO
     * @return ID corresponding to the saved wallet.
     */
    @PostMapping("/create")
    fun createWallet(@RequestBody userID: String?): ResponseEntity<String?> {

        println("WalletController.creteWallet: a new wallet for the user ${userID} is requested")
        val walletID = walletService.createWallet(userID)
        return when(walletID.responseId) {
            ResponseType.USER_WALLET_CREATED -> ResponseEntity(walletID.body as String, HttpStatus.OK)
            ResponseType.USER_WALLET_FAILED -> ResponseEntity(null, HttpStatus.NOT_ACCEPTABLE)

            else -> ResponseEntity(null, HttpStatus.NOT_FOUND)
        }

    }
}
