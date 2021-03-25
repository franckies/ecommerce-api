package it.polito.master.ap.group6.ecommerce.walletservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.TransactionRepository
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


interface WalletService {
    fun createTransaction(placedtransaction: TransactionDTO?, transactionID: String?): Response
    fun undoTransaction(orderID: String?): Response
    fun checkTransaction(checkTransaction: TransactionDTO?, userID: String?): Response
    fun createRecharge(placedRecharge: RechargeDTO?, userID: String?): Response
    fun getWallet(userID: String?): Response
    fun createWallet(userID: String?): Response
}

/**
 * The order service. Implements the business logic.
 * @param walletRepository a reference to the wallet Repository handling the database interaction.
 * @param transactionRepository a reference to the transaction Repository handling the database interaction.
 * @author Andrea Biondo
 */
@Service
class WalletServiceImpl(
    @Autowired private val walletRepository: WalletRepository,
    @Autowired private val transactionRepository: TransactionRepository
) : WalletService {

    override fun createTransaction(placedtransaction: TransactionDTO?, transactionID: String?): Response {

        var res: Response

        try {
            val transaction = placedtransaction?.toModel()
            val wallet = walletRepository.findByUserId(transaction?.userID!!)


            if(transaction.status== TransactionStatus.ACCEPTED){

                wallet.transactions?.find{it.id==transactionID}?.status = TransactionStatus.ACCEPTED

            }
            else if (transaction.status==TransactionStatus.REFUSED) {

                wallet.total = wallet.total!! + transaction.amount!!
                wallet.transactions?.find{it.id==transactionID}?.status = TransactionStatus.REFUSED


            }

            walletRepository.save(wallet)
            val transactionSaved = transactionRepository.save(transaction)

            res =  Response.userWalletConfirmTransaction()
            res.body = transactionSaved.id!!


        }
        catch (e:Exception) {

            res =  Response.userWalletFailed()

        }

        return res
    }

    override fun undoTransaction(orderID: String?): Response {

        var res: Response

        try {

            val transaction = transactionRepository.findByCausal(orderID!!)
            val wallet = walletRepository.findByUserId(transaction.userID!!)

            if(transaction.status == TransactionStatus.ACCEPTED || transaction.status == TransactionStatus.PENDING){

                wallet.total = wallet.total!! + transaction.amount!!
                wallet.transactions?.find{it.id==orderID}?.status = TransactionStatus.REFUNDED

            }

            walletRepository.save(wallet)
            val transactionSaved = transactionRepository.save(transaction)

            res =  Response.userWalletConfirmTransaction()
            res.body = transactionSaved.id!!


        }
        catch (e:Exception) {

            res =  Response.userWalletFailed()

        }

        return res
    }

    override fun checkTransaction(checkTransaction: TransactionDTO?, userID: String?): Response {


        var res: Response

        try {
            val wallet = walletRepository.findByUserId(userID!!)
            val transaction = checkTransaction?.toModel()


            if (wallet.total!! >= checkTransaction?.amount!!) {

                val transactionSaved = transactionRepository.save(transaction!!)
                wallet.total = wallet.total!! - transaction.amount!!
                wallet.transactions.apply {
                    this!!.add(transactionSaved)
                }
                walletRepository.save(wallet)

                res =  Response.userWalletPostTransaction()
                res.body = transactionSaved.id!!

            }
            else {

                transaction?.status = TransactionStatus.REFUSED
                val transactionSaved = transactionRepository.save(transaction!!)
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

    override fun createRecharge(placedRecharge: RechargeDTO?, userID: String?): Response {

        var res: Response

        try {

            val transaction = placedRecharge?.toModel()
            val wallet = walletRepository.findByUserId(userID!!)
            val transactionSaved = transactionRepository.save(transaction!!)

            transaction.status = TransactionStatus.ACCEPTED

            wallet.total = wallet.total!! + transaction.amount!!

            wallet.transactions.apply {
                this!!.add(transactionSaved)
            }

            walletRepository.save(wallet)
            res =  Response.userWalletRecharge()
            res.body = transaction.id

        }
        catch (e:Exception) {

            res =  Response.userWalletFailed()

        }

        return res

    }

    override fun getWallet(userID: String?): Response {

        var res: Response

        try {

            val wallet = walletRepository.findByUserId(userID!!)
            res =  Response.userWalletGet()
            res.body = wallet

        }
        catch (e:Exception) {

            res =  Response.userWalletFailed()

        }

        return res

    }

    override fun createWallet(userID: String?): Response {

        var res: Response

        try {

            walletRepository.findByUserId(userID!!)
            res =  Response.userWalletCreatedFailed()

        }
        catch (e:Exception) {

            val wallet = Wallet(null, userID, 0.0f, mutableListOf())
            val walletSaved = walletRepository.save(wallet)
            res = Response.userWalletCreated()
            res.body = walletSaved.id


        }

        return res

    }


}
