package it.polito.master.ap.group6.ecommerce.catalogservice

import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.UserRepository
import it.polito.master.ap.group6.ecommerce.catalogservice.services.UserService
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CatalogserviceApplication(
	userService: UserService,
	walletService: WalletService
) {
	init {
		// clear table
		userService.clear()

		// populate User table
		val userList = mutableListOf<User>(
			userService.create("Nicolò", "Chiapello", "nico", "123", "Corso Duca degli Abruzzi"),
			userService.create("Francesco", "Semeraro", "fra", "456", "Headquarter K1"),
			userService.create("Andrea", "Biondo", "andre", "789"),
			userService.create("Raffaele", "Martone", "raffa", "741"),
			userService.create("Govanni", "Malnati", "giova", "963", role=UserRole.ADMIN)
		)

		// inform the WalletService
		userList.forEach { user ->
			walletService.createWallet(user)
		}

	}
}

fun main(args: Array<String>) {
	runApplication<CatalogserviceApplication>(*args)
}
