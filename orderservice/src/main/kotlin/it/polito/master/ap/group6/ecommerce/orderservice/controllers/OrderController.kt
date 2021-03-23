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

/**
 * The order controller. Exposes the endpoints.
 * @param orderService a reference to the Service handling the business logic.
 * @author Francesco Semeraro
 */
@RestController
@RequestMapping("/order")
class OrderController(
    @Autowired private val orderService: OrderService,
) {
    /**
     * POST an order into the database.
     * @return The saved order. It will have as status PAID if it is submitted successfully,
     * FAILED otherwise.
     */
    @PostMapping("/orders")
    //@RolesAllowed("SERVICE") //TODO: check if it is like this
    fun createOrder(@RequestBody placedOrder: PlacedOrderDTO): ResponseEntity<OrderDTO> {
        val order = orderService.createOrder(placedOrder)
        return ResponseEntity.ok(order!!.toDto())
    }

    /**
     * GET the order having orderID as identifier.
     * @return A list of OrderDTO, each one corresponding to a delivery associated to the requested order.
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/orders/{orderID}")
    fun getOrder(@PathVariable("orderID") orderID: String): List<OrderDTO>? {
        try {
            val orderList = orderService.getOrder(ObjectId(orderID)) ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "The order $orderID cannot be found"
            )
            return orderList.map { it.toDto() }
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "The order $orderID cannot be found")
        }
    }

    /**
     * GET all the order of a specific user having userID as identifier.
     * @return A list of the DTOs corresponding to the orders of the user.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exists.
     */
    @GetMapping("/{userID}/orders/")
    fun getOrdersByUser(@PathVariable("userID") userID: String): List<List<OrderDTO>>? {
        val ordersList = orderService.getOrdersByUser(userID) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "The user $userID cannot be found"
        )
        return ordersList.map { it.map { it.toDto() } }
    }

    /**
     * DELETE an order from the database. It actually modifies its status to CANCELED.
     * @return the DTO corresponding to the canceled order.
     * @throws HttpStatus.FORBIDDEN if the order cannot be canceled (i.e. it has been delivered)
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @DeleteMapping("orders/{orderID}")
    fun cancelOrder(@PathVariable("orderID") orderID: String): OrderDTO? {
        try {
            val canceledOrder = orderService.cancelOrder(ObjectId(orderID))
            if (canceledOrder.isPresent) {
                if (canceledOrder.get().status == OrderStatus.CANCELED) {
                    return canceledOrder.get().toDto()
                } else {
                    throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "The order $orderID cannot be canceled because it has been already delivered"
                    )
                }
            } else {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "The order $orderID cannot be found")
            }
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "The order $orderID cannot be found")
        }
    }
}