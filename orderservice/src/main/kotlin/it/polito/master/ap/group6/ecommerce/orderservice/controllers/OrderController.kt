package it.polito.master.ap.group6.ecommerce.orderservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.services.OrderService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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
    fun createOrder(@RequestBody placedOrder: PlacedOrderDTO): OrderDTO? {
        println("OrderController.createOrder: a new order from the user ${placedOrder.user!!.id} is requested.")
        return orderService.createOrder(placedOrder)
    }

    /**
     * GET the order having orderID as identifier.
     * @return A list of OrderDTO, each one corresponding to a delivery associated to the requested order.
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/orders/{orderID}")
    fun getOrder(@PathVariable("orderID") orderID: String): List<OrderDTO>? {
        println("OrderController.getOrder: information about the order $orderID is requested.")
        try {
            val orderList: List<OrderDTO> = orderService.getOrder(ObjectId(orderID)) ?: run {
                println("OrderController.getOrder: The order $orderID cannot be found")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "The order $orderID cannot be found")
            }
            return orderList
        } catch (e: IllegalArgumentException) {
            println("OrderController.getOrder: The order $orderID cannot be found.")
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
        println("OrderController.getOrderByUser: information about the orders of the user $userID is requested.")
        val ordersList: List<List<OrderDTO>> = orderService.getOrdersByUser(userID) ?: run {
            println("OrderController.getOrderByUser: The user $userID cannot be found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "The user $userID cannot be found")
        }
        return ordersList
    }

    /**
     * DELETE an order from the database. It actually modifies its status to CANCELED.
     * @return the DTO corresponding to the canceled order.
     * @throws HttpStatus.FORBIDDEN if the order cannot be canceled (i.e. it has been delivered)
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @DeleteMapping("orders/{orderID}")
    fun cancelOrder(@PathVariable("orderID") orderID: String): OrderDTO? {
        println("OrderController.cancelOrder: the order $orderID is requested to be canceled.")
        try {
            val canceledOrder: OrderDTO = orderService.cancelOrder(ObjectId(orderID))
            if (canceledOrder.status == OrderStatus.CANCELED) {
                return canceledOrder
            } else {
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN, "The order $orderID cannot be canceled because it has been already delivered"
                )
            }
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "The order $orderID cannot be found")
        }
    }
}