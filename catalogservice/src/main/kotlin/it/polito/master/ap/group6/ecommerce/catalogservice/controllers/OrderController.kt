//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.controllers

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import mu.KotlinLogging
import org.bson.types.ObjectId
import java.lang.IllegalArgumentException

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
     * This exploits a synchronous workflow, with OrderService behaving as orchestrator.
     * @return the representation of the created order (with ID and status).
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @PostMapping("{userID}/sync")
    fun createOrderSync(@PathVariable("userID") userID: String,  //TODO enhance by retrieving userID by the logged credentials
                    @RequestBody placedOrderDTO: PlacedOrderDTO): ResponseEntity<OrderDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received POST on url='${currentRequest?.requestURL}' with body=${placedOrderDTO}" }

        // cast input parameters
        val user_id: ObjectId = try {
            ObjectId(userID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $userID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val created_order = orderService.createOrderSync(user_id, placedOrderDTO)

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
     * Create an order for the userID user with the details specified in PlacedOrderDTO.
     * This exploits an asynchronous workflow, with Kafka behaving as choreography broker.
     * @return the representation of the created order (with ID and status).
     */
    @PostMapping("{userID}/async")
    fun createOrderAsync(@PathVariable("userID") userID: String,  //TODO enhance by retrieving userID by the logged credentials
                    @RequestBody placedOrderDTO: PlacedOrderDTO): ResponseEntity<Any?> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received POST on url='${currentRequest?.requestURL}' with body=${placedOrderDTO}" }

        // cast input parameters
        val user_id: ObjectId = try {
            ObjectId(userID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $userID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val empty_placeholder = orderService.createOrderAsync(user_id, placedOrderDTO)

        // check the result
        return when (empty_placeholder.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(empty_placeholder.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, empty_placeholder.http_code!!)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(empty_placeholder.body, HttpStatus.INTERNAL_SERVER_ERROR)
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

        // cast input parameters
        val user_id: ObjectId = try {
            ObjectId(userID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $userID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val placed_order_list_dto = orderService.readOrderHistory(user_id)

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
     * This exploits a synchronous workflow, with OrderService behaving as orchestrator.
     * @return the DTO corresponding to the cancelled order.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @GetMapping("/delete/{orderID}/sync")
    fun undoOrderSync(@PathVariable("orderID") orderID: String): ResponseEntity<OrderDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received DELETE on url='${currentRequest?.requestURL}'" }

        // cast input parameters
        val order_id: ObjectId = try {
            ObjectId(orderID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $orderID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val cancelled_order = orderService.undoOrderSync(order_id)

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


    /**
     * Cancel an order orderID for the currently logged user (update its STATUS).
     * This exploits an asynchronous workflow, with Kafka behaving as choreography broker.
     * @return the DTO corresponding to the cancelled order.
     */
    @GetMapping("/delete/{orderID}/async")
    fun undoOrderAsync(@PathVariable("orderID") orderID: String): ResponseEntity<OrderDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received DELETE on url='${currentRequest?.requestURL}'" }

        // cast input parameters
        val order_id: ObjectId = try {
            ObjectId(orderID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $orderID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val cancelled_order = orderService.undoOrderAsync(order_id)

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
