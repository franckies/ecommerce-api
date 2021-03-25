//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import springfox.documentation.swagger2.annotations.EnableSwagger2

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.catalogservice.services.UserService
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import org.springframework.context.support.beans
import java.util.*


//======================================================================================================================
//   SpringBoot Application
//======================================================================================================================
@SpringBootApplication
@EnableSwagger2
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
			userService.create("Govanni", "Malnati", "giova", "963", role = UserRole.ADMIN)
		)

		// inform the WalletService
		userList.forEach { user ->
			if (user.role == UserRole.CUSTOMER){
				walletService.createWallet(user)
				walletService.issueRecharge(user.id!!, RechargeDTO(user.toDto(), 10000f, Date(), "initial recharge"))
			}

		}
	}
}



//======================================================================================================================
//   Entrypoint
//======================================================================================================================
fun main(args: Array<String>) {
	runApplication<CatalogserviceApplication>(*args){

		addInitializers( beans {
			bean {

				// create login credentials
				fun user(user: String, pw: String, vararg roles: String) =
					org.springframework.security.core.userdetails.User.withDefaultPasswordEncoder()
						.username(user).password(pw).roles(*roles).build()

				InMemoryUserDetailsManager(
					user("nico", "123", UserRole.CUSTOMER.toString()),
					user("fra", "456", UserRole.CUSTOMER.toString()),
					user("andre", "789", UserRole.CUSTOMER.toString()),
					user("raffa", "741", UserRole.CUSTOMER.toString()),
					user("giova", "963", UserRole.ADMIN.toString())
				)
			}
		})
	}
}
