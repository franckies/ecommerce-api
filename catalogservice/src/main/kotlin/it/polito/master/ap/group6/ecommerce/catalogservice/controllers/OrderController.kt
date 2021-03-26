//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.controllers

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import mu.KotlinLogging

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.OrderService
import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.ShownOrderListDTO
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType


//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * Exposes Catalog endpoints, but aimed to the Order microservice.
 * @param orderService a reference to the Service handling the business logic.
 *
 * @author Nicolò Chiapello
 */
@RestController
@RequestMapping("/catalog/orders")  // root endpoint
class OrderController(
    @Autowired private val orderService: OrderService
) {
    //------- attributes -------------------------------------------------------
    private val logger = KotlinLogging.logger {}


    //------- methods ----------------------------------------------------------
    /**
     * Create an order for the userID user with the details specified in PlacedOrderDTO.
     * @return the representation of the created order (with ID and status).
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @PostMapping("{userID}")
    fun createOrder(@PathVariable("userID") userID: String,  //TODO enhance by retrieving userID by the logged credentials
                    @RequestBody placedOrderDTO: PlacedOrderDTO): ResponseEntity<OrderDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received POST on url='${currentRequest?.requestURL}' with body=${placedOrderDTO}" }

        // invoke the business logic
        val created_order = orderService.createOrder(userID, placedOrderDTO)

        // check the result
        return when (created_order.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(created_order.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, created_order.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(created_order.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Shows the orders associated with given user.
     * @return the DTO corresponding to the list of all the submitted orders.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @GetMapping("/{userID}")
    fun readOrderHistory(@PathVariable("userID") userID: String): ResponseEntity<ShownOrderListDTO?> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received GET on url='${currentRequest?.requestURL}'" }

        // invoke the business logic
        val placed_order_list_dto = orderService.readOrderHistory(userID)

        // check the result
        return when (placed_order_list_dto.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(placed_order_list_dto.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, placed_order_list_dto.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(placed_order_list_dto.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Cancel an order orderID for the currently logged user (update its STATUS).
     * @return the DTO corresponding to the cancelled order.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @GetMapping("/delete/{orderID}")
    fun undoOrder(@PathVariable("orderID") orderID: String): ResponseEntity<OrderDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received DELETE on url='${currentRequest?.requestURL}'" }

        // invoke the business logic
        val cancelled_order = orderService.undoOrder(orderID)

        // check the result
        return when (cancelled_order.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(cancelled_order.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, cancelled_order.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            else -> ResponseEntity(cancelled_order.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}
