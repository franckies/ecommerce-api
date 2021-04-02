//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResult
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType
import it.polito.master.ap.group6.ecommerce.catalogservice.models.Operation
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.OperationRepository
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import org.springframework.kafka.core.KafkaTemplate


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface MailingService {
    fun createUserMail(user: User): ExecutionResult<String>
}


//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic dealing with the external Mailing microservice.
 * @property userService a reference to the Service handling the User CRUD operations.
 * @property mailingservice_url the URL of the external Mailing microservice, configurable by property file.
 *
 * @author Nicolò Chiapello
 */
@Service
class MailingServiceImpl(
    @Autowired private val userService: UserService,
    @Value("\${application.mailing_service}") private var mailingservice_url: String = "localhost:8085"
) : MailingService {

    //------- attributes -------------------------------------------------------
    @Autowired
    lateinit var operationRepository: OperationRepository

    val mapper = jacksonObjectMapper()


    //------- methods ----------------------------------------------------------
    override fun createUserMail(user: User): ExecutionResult<String> {
        // prepare data to submit
        val user_dto: UserDTO = user.toDto()
        val url: String = "http://${mailingservice_url}/mailing/create"

        // submit remotely to the WalletService microservice
        var res: String? = null
        try {
            print("Performing POST on '$url'... ")
            res = RestTemplate().postForObject(
                url,  // url
                user_dto,  // request body
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
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = res)
    }




    //------- internal facilities ----------------------------------------------

}
