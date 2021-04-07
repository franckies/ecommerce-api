package it.polito.master.ap.group6.ecommerce.walletservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Transaction
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.TransactionRepository
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface WalletServiceAsync {
    fun createTransaction(placedOrder: PlacedOrderDTO?): Response
}

/**
 * The order service. Implements the business logic.
 * @param walletRepository a reference to the wallet Repository handling the database interaction.
 * @param transactionRepository a reference to the transaction Repository handling the database interaction.
 * @author Andrea Biondo
 */
@Service
@Transactional
class WalletServiceAsyncImpl(
        @Autowired private val walletRepository: WalletRepository,
        @Autowired private val transactionRepository: TransactionRepository
) : WalletServiceAsync {

    override fun createTransaction(placedOrder: PlacedOrderDTO?): Response {

        var res: Response

        val amount: Float? = placedOrder?.purchaseList?.map { it.sellingPrice!! * it.quantity!!.toFloat() }?.sum()

        try {
            val wallet = walletRepository.findByUserID(placedOrder?.userID.toString())
            val transaction = Transaction(null, placedOrder?.sagaID, placedOrder?.userID, amount, Date(), causal = "OrderID: ${placedOrder?.sagaID}", null)


            if (wallet.total!! >= amount!!) {

                transaction.status = TransactionStatus.ACCEPTED
                val transactionSaved = transactionRepository.save(transaction)
                wallet.total = wallet.total!! - transaction.amount!!
                wallet.transactions.apply {
                    this!!.add(transactionSaved)
                }
                walletRepository.save(wallet)

                res =  Response.userWalletPostTransaction()
                res.body = transactionSaved.id!!

            }
            else {

                transaction.status = TransactionStatus.REFUSED
                val transactionSaved = transactionRepository.save(transaction)
                wallet.transactions.apply {
                    this!!.add(transactionSaved)
                }
                walletRepository.save(wallet)
                res =  Response.userWalletNoMoney()

            }
        }
        catch (e:Exception) {

            res =  Response.userWalletFailed()

        }

        return res

    }

}