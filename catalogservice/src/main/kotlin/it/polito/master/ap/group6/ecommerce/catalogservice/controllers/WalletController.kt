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

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WalletService
import it.polito.master.ap.group6.ecommerce.common.dtos.RechargeDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.WalletDTO


//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * Exposes catalog endpoints, but aimed to the wallet-service microservice.
 * @param walletService a reference to the Service handling the business logic.
 *
 * @author Nicolò Chiapello
 */
@RestController
@RequestMapping("/catalog/wallet")  // root endpoint
class WalletController(
    @Autowired private val walletService: WalletService
) {

    /**
     * Retrieve the wallet information (total and transaction list) for the currently logged user.
     * @return the DTO corresponding to the wallet+transactions of the given user.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist.
     */
    @GetMapping("/{userID}")
    fun getEconomicInformation(@PathVariable("userID") userID: String): WalletDTO {

        // invoke the business logic
        val wallet_dto = walletService.askEconomicInformation(userID)

        // check the result
        if (wallet_dto.isPresent)
            return wallet_dto.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Recharge a given amount of money for the selected user. Permitted only for the administrators.
     * @return the HTTP code for success or vailure.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist.
     */
    @PostMapping("/admin/recharge/{userID}")
    fun issueRecharge(@PathVariable("userID") userID: String,
                      @RequestBody rechargeDto: RechargeDTO): HttpStatus {

        // invoke the business logic
        val success: Boolean = walletService.issueRecharge(userID, rechargeDto)

        // check the result
        if (success)
            return HttpStatus.OK
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}