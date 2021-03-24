package it.polito.master.ap.group6.ecommerce.walletservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Transaction
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.TransactionRepository
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


interface WalletService {
    fun createTransaction(placedtransaction: TransactionDTO, transactionID: String): String
    fun checkTransaction(checkTransaction: TransactionDTO, userID: String): String?
    fun createRecharge(placedRecharge: RechargeDTO, userID: String): String
    fun getWallet(userID: String): Wallet
    fun createWallet(user: UserDTO): String
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
    override fun createTransaction(placedtransaction: TransactionDTO, transactionID: String): String {

        val transaction = placedtransaction.toModel()
        val wallet = walletRepository.findByUserId(transaction.user?.id!!)


        if(transaction.status== TransactionStatus.ACCEPTED){

            wallet.transactions?.find{it.id==transactionID}?.status = TransactionStatus.ACCEPTED

        }
        else if (transaction.status==TransactionStatus.REFUSED) {

            wallet.total = wallet.total!! + transaction.amount!!
            wallet.transactions?.find{it.id==transactionID}?.status = TransactionStatus.REFUSED


        }

        walletRepository.save(wallet)
        val transactionSaved = transactionRepository.save(transaction)

        return transactionSaved.id!!

    }

    override fun checkTransaction(checkTransaction: TransactionDTO, userID: String): String? {

        val wallet = walletRepository.findByUserId(userID)
        val transaction = checkTransaction.toModel()


        if (wallet.total!! >= checkTransaction.amount!!) {

            val transactionSaved = transactionRepository.save(transaction)
            wallet.total = wallet.total!! - transaction.amount!!
            wallet.transactions.apply {
                this!!.add(transactionSaved)
            }
            walletRepository.save(wallet)
            return transactionSaved.id!!

        }
        else {

            transaction.status = TransactionStatus.REFUSED
            val transactionSaved = transactionRepository.save(transaction)
            wallet.transactions.apply {
                this!!.add(transactionSaved)
            }
            walletRepository.save(wallet)
            return null

        }



    }

    override fun createRecharge(placedRecharge: RechargeDTO, userID: String): String {

        val transaction = placedRecharge.toModel()
        val wallet = walletRepository.findByUserId(userID)
        val transactionSaved = transactionRepository.save(transaction)

        transaction.status = TransactionStatus.ACCEPTED

        wallet.total = wallet.total!! + transaction.amount!!

        wallet.transactions.apply {
            this!!.add(transactionSaved)
        }

        walletRepository.save(wallet)

        return transactionSaved.id!!

    }

    override fun getWallet(userID: String): Wallet {
        return walletRepository.findByUserId(userID)
    }

    override fun createWallet(user: UserDTO): String {

        val wallet = Wallet(null,user,0.0f, mutableListOf<Transaction>())
        val walletSaved = walletRepository.save(wallet)

        return walletSaved.id!!

    }


}
