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

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.OrderService
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO



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

    /**
     * Create an order for the userID user with the details specified in PlacedOrderDTO.
     * @return the representation of the created order (with ID and status).
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @PostMapping("{userID}")
    fun createOrder(@PathVariable("userID") userID: String,  //TODO enhance by retrieving userID by the logged credentials
                    @RequestBody placedOrderDTO: PlacedOrderDTO): OrderDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received POST on url='${currentRequest?.requestURL}' with body=${placedOrderDTO}")

        // invoke the business logic
        val created_order = orderService.createOrder(userID, placedOrderDTO)

        // check the result
        if (created_order.isPresent)
            return created_order.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Shows the orders associated with given user.
     * @return the DTO corresponding to the list of all the submitted orders.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @GetMapping("/{userID}")
    fun readOrderHistory(@PathVariable("userID") userID: String): PlacedOrderListDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received GET on url='${currentRequest?.requestURL}'")

        // invoke the business logic
        val placed_order_list_dto = orderService.readOrderHistory(userID)

        // check the result
        if (placed_order_list_dto.isPresent)
            return placed_order_list_dto.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Cancel an order orderID for the currently logged user (update its STATUS).
     * @return the DTO corresponding to the cancelled order.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exist or the remote microservice doesn't respond.
     */
    @DeleteMapping("/{orderID}")
    fun undoOrder(@PathVariable("orderID") orderID: String): OrderDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received DELETE on url='${currentRequest?.requestURL}'")

        // invoke the business logic
        val cancelled_order = orderService.undoOrder(orderID)

        // check the result
        if (cancelled_order.isPresent)
            return cancelled_order.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}
