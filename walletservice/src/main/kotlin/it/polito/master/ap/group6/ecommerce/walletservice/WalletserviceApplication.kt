package it.polito.master.ap.group6.ecommerce.walletservice

import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionStatus
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Transaction
import it.polito.master.ap.group6.ecommerce.walletservice.models.dtos.Wallet
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.TransactionRepository
import it.polito.master.ap.group6.ecommerce.walletservice.repositories.WalletRepository

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@SpringBootApplication
@EnableSwagger2
class WalletserviceApplication(
    walletRepo: WalletRepository,
    transactionRepo: TransactionRepository
) {
    init {
        //clear table
        walletRepo.deleteAll()
        val userDTOList = mutableListOf<UserDTO>().apply {
            add(UserDTO("1239820421", "Francesco", "Semeraro", "Milano", "User"))
            add(UserDTO("2142109842", "Nicolò", "Chiapello", "Torino", "User"))
        }


        val transactionList = mutableListOf<Transaction>().apply {
            add(Transaction("0000000000",userDTOList[0],21.99, Calendar.getInstance(), "1111111111", TransactionStatus.ACCEPTED))
        }

        //populate product table
        val walletList = mutableListOf<Wallet>().apply {
            add(Wallet(userDTOList[0], 30.20, transactionList))
        }
        walletRepo.saveAll(walletList)

    }

}


fun main(args: Array<String>) {
    runApplication<WalletserviceApplication>(*args)
}
