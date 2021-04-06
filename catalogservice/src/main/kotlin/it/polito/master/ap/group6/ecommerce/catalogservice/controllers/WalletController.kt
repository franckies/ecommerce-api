//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.controllers

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import mu.KotlinLogging
import java.lang.IllegalArgumentException
import javax.annotation.security.RolesAllowed

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType


//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * Exposes Catalog endpoints, but aimed to the Wallet microservice.
 * @param walletService a reference to the Service handling the business logic.
 *
 * @author Nicolò Chiapello
 */
@RestController
@RequestMapping("/catalog/wallet")  // root endpoint
class WalletController(
    @Autowired private val walletService: WalletService
) {
    //------- attributes -------------------------------------------------------
    private val logger = KotlinLogging.logger {}


    //------- methods ----------------------------------------------------------
    /**
     * Retrieve the wallet information (total and transaction list) for the currently logged user.
     * @return the DTO corresponding to the wallet+transactions of the given user.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist.
     */
    @GetMapping("/{userID}")
    fun getEconomicInformation(@PathVariable("userID") userID: String): ResponseEntity<WalletDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received GET on url='${currentRequest?.requestURL}'" }

        // cast input parameters
        val user_id: ObjectId = try {
            ObjectId(userID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $userID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val wallet_dto = walletService.askEconomicInformation(user_id)

        // check the result
        return when (wallet_dto.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(wallet_dto.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, wallet_dto.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(wallet_dto.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Recharge a given amount of money for the selected user. Permitted only for the administrators.
     * @return the HTTP code for success or failure.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist.
     */
    @RolesAllowed("ROLE_ADMIN")
    @PostMapping("/admin/recharge/{userID}")
    fun issueRecharge(@PathVariable("userID") userID: String,
                      @RequestBody rechargeDto: RechargeDTO): ResponseEntity<String> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received POST on url='${currentRequest?.requestURL}' with body=${rechargeDto}" }

        // cast input parameters
        val user_id: ObjectId = try {
            ObjectId(userID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $userID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val transaction_id = walletService.issueRecharge(user_id, rechargeDto)

        // check the result
        return when (transaction_id.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(transaction_id.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, transaction_id.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(transaction_id.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}
