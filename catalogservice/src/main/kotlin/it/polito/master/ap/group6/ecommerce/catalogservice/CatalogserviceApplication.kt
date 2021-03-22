package it.polito.master.ap.group6.ecommerce.catalogservice

import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.UserRepository
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CatalogserviceApplication(
	userRepository: UserRepository
) {
	init {
		// clear table
		userRepository.deleteAll()

		// populate User table
		userRepository.save(User("Nicolò", "Chiapello", "nico", "123", "Corso Duca degli Abruzzi"))
		userRepository.save(User("Francesco", "Semeraro", "fra", "456", "Headquarter K1"))
		userRepository.save(User("Andrea", "Biondo", "andre", "789"))
		userRepository.save(User("Raffaele", "Martone", "raffa", "741"))
		userRepository.save(User("Govanni", "Malnati", "giova", "963", role=UserRole.ADMIN))
	}
}

fun main(args: Array<String>) {
	runApplication<CatalogserviceApplication>(*args)
}
