//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.ResourceAccessException
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO



//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface WalletService {
    fun createWallet(user: User): Boolean
    fun askEconomicInformation(userID: String): Optional<WalletDTO>
    fun issueRecharge(userID: String, rechargeDto: RechargeDTO): Boolean
}



//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic dealing with the external Wallet microservice.
 * @property userService a reference to the Service handling the User CRUD operations.
 * @property walletservice_url the URL of the external WalletService microservice, configurable by property file.
 *
 * @author Nicolò Chiapello
 */
@Service
class WalletServiceImpl(
    @Autowired private val userService: UserService,
    @Value("\${application.wallet_service}") private var walletservice_url: String = "localhost:8083"
) : WalletService {

    override fun createWallet(user: User): Boolean {
        // prepare data to submit
        val user_dto: UserDTO = user.toDto()
        val url: String = "http://${walletservice_url}/wallet/create"

        // ask remotely to the WalletService microservice
        var wallet_id: String? = null
        try {
             wallet_id = RestTemplate().postForObject(  //TODO convert in PUT
                url,  // url
                user_dto,  // request
                String::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to POST on '$url' the object:\n$user_dto")
            return false
        }

        // provide outcome
        return wallet_id != null
    }

    override fun askEconomicInformation(userID: String): Optional<WalletDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return Optional.empty()

        // ask remotely to the WalletService microservice
        val url: String = "http://${walletservice_url}/wallet/${user.get().id}"
        var res: WalletDTO? = null
        try {
            res = RestTemplate().getForObject(
                url,  // url
                WalletDTO::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to GET on '$url'")
            return Optional.empty()
        }

        // provide requested outcome
        if (res == null)
            return Optional.empty()
        else
            return Optional.of(res)
    }


    override fun issueRecharge(userID: String, rechargeDto: RechargeDTO): Boolean {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return false

        // ask remotely to the WalletService microservice
        val url: String = "http://${walletservice_url}/wallet/${user.get().id}"
        val transaction_id: String? = null
        try {
            RestTemplate().postForObject(
                url,  // url
                rechargeDto,  // request
                String::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to POST on '$url' the object:\n$rechargeDto")
            return false
        }

        // provide requested outcome
        return transaction_id != null
    }
}