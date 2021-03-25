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
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResult
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface WalletService {
    fun createWallet(user: User): ExecutionResult<String>
    fun askEconomicInformation(userID: String): ExecutionResult<WalletDTO>
    fun issueRecharge(userID: String, rechargeDto: RechargeDTO): ExecutionResult<String>
}



//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic dealing with the external Wallet microservice.
 * @property userService a reference to the Service handling the User CRUD operations.
 * @property walletservice_url the URL of the external Wallet microservice, configurable by property file.
 *
 * @author Nicolò Chiapello
 */
@Service
class WalletServiceImpl(
    @Autowired private val userService: UserService,
    @Value("\${application.wallet_service}") private var walletservice_url: String = "localhost:8083"
) : WalletService {

    override fun createWallet(user: User): ExecutionResult<String> {
        // prepare data to submit
        val user_dto: UserDTO = user.toDto()
        val url: String = "http://${walletservice_url}/wallet/create"

        // submit remotely to the WalletService microservice
        var wallet_id: String? = null
        try {
            print("Performing POST on '$url'... ")
            wallet_id = RestTemplate().postForObject(
                url,  // url
                user_dto,  // request
                String::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Wallet service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = wallet_id)
    }

    override fun askEconomicInformation(userID: String): ExecutionResult<WalletDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "User $userID does not exist")

        // ask remotely to the WalletService microservice
        val url: String = "http://${walletservice_url}/wallet/${user.get().id}"
        var res: WalletDTO? = null
        try {
            res = RestTemplate().getForObject(
                url,  // url
                WalletDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Wallet service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = res)
    }


    override fun issueRecharge(userID: String, rechargeDto: RechargeDTO): ExecutionResult<String> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "User $userID does not exist")

        // submit remotely to the WalletService microservice
        val url: String = "http://${walletservice_url}/wallet/recharge/${user.get().id}"
        var transaction_id: String? = null
        try {
            transaction_id = RestTemplate().postForObject(
                url,  // url
                rechargeDto,  // request
                String::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Wallet service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = transaction_id)
    }
}
