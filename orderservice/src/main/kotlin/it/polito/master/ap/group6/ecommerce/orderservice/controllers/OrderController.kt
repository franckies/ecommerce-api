package it.polito.master.ap.group6.ecommerce.orderservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.services.OrderService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import javax.annotation.security.RolesAllowed

/**
 * The order controller. Exposes the endpoints.
 * @param orderService a reference to the Service handling the business logic.
 * @author Francesco Semeraro
 */
@RestController
@RequestMapping("/order")
class OrderController(
    @Autowired private val orderService: OrderService
) {
    /**
     * POST an order into the database.
     * @return DTO corresponding to the saved order.
     */
    @PostMapping("/orders")
    //@RolesAllowed("SERVICE") //TODO: check if it is like this
    fun createOrder(@RequestBody placedOrder: PlacedOrderDTO): ResponseEntity<OrderDTO> {
        val order = orderService.createOrder(placedOrder)
        return ResponseEntity.ok(order.toDto())
    }

    /**
     * GET the order having orderID as identifier.
     * @return the DTO corresponding to the retrieved order.
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/orders/{orderID}")
    fun getOrder(@PathVariable("orderID") orderID: String): OrderDTO {
        //TODO: ObjectId generates exception if id is not valid hex.
        val order = orderService.getOrder(ObjectId(orderID))
        if (order.isPresent) {
            return order.get().toDto()
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * GET the status of the order having orderID as identifier.
     * @return the order status.
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/orders/{orderID}/status")
    fun getOrderStatus(@PathVariable("orderID") orderID: String): OrderStatus? {
        //TODO: ObjectId generates exception if id is not valid hex.
        val order = orderService.getOrder(ObjectId(orderID))
        if (order.isPresent) {
            return order.get().status
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * GET all the order of a specific user having userID as identifier.
     * @return A list of the DTOs corresponding to the orders of the user.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exists.
     */
    @GetMapping("/{userID}/orders/")
    fun getOrdersByUser(@PathVariable("userID") userID: String): List<OrderDTO> {
        val orders = orderService.getOrdersByUser(userID)
        if (orders.all { it.isPresent }) {
            return orders.map { it.get().toDto() }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * DELETE an order from the database. It actually modifies its status to CANCELED.
     * @return the DTO corresponding to the canceled order.
     * @throws HttpStatus.FORBIDDEN if the order cannot be canceled (i.e. it has been delivered)
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @DeleteMapping("orders/{orderID}")
    fun cancelOrder(@PathVariable("orderID") orderID: String): OrderDTO? {
        //TODO: ObjectId generates exception if id is not valid hex.
        val canceledOrder = orderService.cancelOrder(ObjectId(orderID))
        if (canceledOrder.isPresent) {
            if (canceledOrder.get().status == OrderStatus.CANCELLED) {
                return canceledOrder.get().toDto()
            } else {
                throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        } else {
            ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return null
    }
}