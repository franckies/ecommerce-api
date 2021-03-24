package it.polito.master.ap.group6.ecommerce.walletservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Transaction
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.walletservice.services.WalletService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
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
    @PostMapping("/performtransaction/{transactionID}")
    fun createTransaction(@RequestBody placedTransaction: TransactionDTO, @PathVariable("transactionID") transactionID: String): ResponseEntity<String?> {

        val transactionResult = walletService.createTransaction(placedTransaction,transactionID)
        return ResponseEntity.ok(transactionResult)

    }

    /**
     * POST a transaction into the database.
     * Check if there are enough money in the userID wallet
     * @return ID corresponding to the saved transaction.
     */
    @PostMapping("/checkavailability/{userID}")
    fun checkTransaction(@RequestBody checkTransaction: TransactionDTO, @PathVariable("userID") userID: String): ResponseEntity<String?> {

        val transactionID = walletService.checkTransaction(checkTransaction,userID)

        if (transactionID!=null)
            return ResponseEntity.ok(transactionID!!)
        else
            return ResponseEntity.ok("")
            TODO("HOW TO RETURN NULL??")

    }

    /**
     * POST a transaction into the database.
     * Recharge a given amount to the userID wallet
     * @return ID corresponding to the saved transaction.
     */
    @PostMapping("/recharge/{userID}")
    fun createRecharge(@RequestBody placedRecharge: RechargeDTO, @PathVariable("userID") userID: String): ResponseEntity<String?> {

        val transactionID = walletService.createRecharge(placedRecharge,userID)
        return ResponseEntity.ok(transactionID)

    }

    /**
     * GET the wallet having orderID as identifier.
     * @return the DTO corresponding to the retrieved wallet.
     */
    @GetMapping("/{userID}")
    fun getWallet(@PathVariable("userID") userID: String): ResponseEntity<WalletDTO> {
        val wallet = walletService.getWallet(userID)
        return ResponseEntity.ok(wallet.toDto())
    }

}
