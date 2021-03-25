package it.polito.master.ap.group6.ecommerce.walletservice

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
        transactionRepo.deleteAll()
        /*val userDTOList = mutableListOf<UserDTO>().apply {
            add(UserDTO("1239820421", "Francesco", "Semeraro", "Milano", "User", UserRole.CUSTOMER.toString()))
            add(UserDTO("2142109842", "Nicolò", "Chiapello", "Torino", "User", UserRole.CUSTOMER.toString()))
        }


        val transactionList = mutableListOf<Transaction>().apply {
            add(Transaction("0000000000",userDTOList[0],21.99f, Date(), "1111111111", TransactionStatus.ACCEPTED))
        }

        //populate product table
        val walletList = mutableListOf<Wallet>().apply {
            add(Wallet("1234567890",userDTOList[0], 30.20f, transactionList))
        }
        walletRepo.saveAll(walletList)*/

    }

}


fun main(args: Array<String>) {
    runApplication<WalletserviceApplication>(*args)
}
