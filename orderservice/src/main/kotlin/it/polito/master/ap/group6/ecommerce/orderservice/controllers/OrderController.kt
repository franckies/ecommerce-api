package it.polito.master.ap.group6.ecommerce.orderservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.ShownOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.ShownOrderListDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.ResponseType
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
    fun createOrder(@RequestBody placedOrder: PlacedOrderDTO): ResponseEntity<OrderDTO?> {
        println("OrderController.createOrder: a new order from the user ${placedOrder.userID} is requested.")
        val createdOrder = orderService.createOrder(placedOrder)
        return when(createdOrder.responseId){
            ResponseType.ORDER_CREATED -> ResponseEntity(createdOrder.body as OrderDTO, HttpStatus.OK)
            ResponseType.NO_PRODUCTS -> ResponseEntity(createdOrder.body as OrderDTO, HttpStatus.CONFLICT)
            ResponseType.NO_MONEY -> ResponseEntity(createdOrder.body as OrderDTO, HttpStatus.PAYMENT_REQUIRED)
            else -> ResponseEntity(createdOrder.body as OrderDTO, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /**
     * GET the order having orderID as identifier.
     * @return A list of OrderDTO, each one corresponding to a delivery associated to the requested order.
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/orders/{orderID}")
    fun getOrder(@PathVariable("orderID") orderID: String): ResponseEntity<ShownOrderDTO?> {
        println("OrderController.getOrder: information about the order $orderID is requested.")
        try {
            val orderList = orderService.getOrder(ObjectId(orderID))
            return when(orderList.responseId){
                ResponseType.ORDER_NOT_FOUND -> ResponseEntity(null, HttpStatus.NOT_FOUND)
                ResponseType.ORDER_FOUND -> ResponseEntity(ShownOrderDTO(orderList.body as List<OrderDTO>?), HttpStatus.OK)
                else -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: IllegalArgumentException) {
            println("OrderController.getOrder: The order $orderID cannot be found because the id is not in a valid format.")
            return ResponseEntity(null, HttpStatus.NOT_FOUND)
        }
    }

    /**
     * GET all the order of a specific user having userID as identifier.
     * @return A list of the DTOs corresponding to the orders of the user.
     * @throws HttpStatus.NOT_FOUND if the user doesn't exists.
     */
    @GetMapping("/{userID}/orders")
    fun getOrdersByUser(@PathVariable("userID") userID: String): ResponseEntity<ShownOrderListDTO?> {
        println("OrderController.getOrderByUser: information about the orders of the user $userID is requested.")
        val ordersList = orderService.getOrdersByUser(userID)
        return when(ordersList.responseId){
            ResponseType.ORDER_FOUND -> ResponseEntity(ShownOrderListDTO(ordersList.body as List<List<OrderDTO>>?), HttpStatus.OK)
            ResponseType.ORDER_NOT_FOUND -> ResponseEntity(ShownOrderListDTO(), HttpStatus.OK) //no orders associated with user, return empty list
            else -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /**
     * DELETE an order from the database. It actually modifies its status to CANCELED.
     * @return the DTO corresponding to the canceled order.
     * @throws HttpStatus.FORBIDDEN if the order cannot be canceled (i.e. it has been delivered)
     * @throws HttpStatus.NOT_FOUND if the order doesn't exist.
     */
    @GetMapping("/delete/{orderID}")
    fun cancelOrder(@PathVariable("orderID") orderID: String): ResponseEntity<OrderDTO?> {
        println("OrderController.cancelOrder: the order $orderID is requested to be canceled.")
        try {
            val canceledOrder = orderService.cancelOrder(ObjectId(orderID))
            return when(canceledOrder.responseId){
                ResponseType.ORDER_FOUND -> ResponseEntity(canceledOrder.body as OrderDTO, HttpStatus.OK)
                ResponseType.ORDER_NOT_FOUND -> ResponseEntity(null, HttpStatus.NOT_FOUND)
                ResponseType.CANNOT_RESTORE_PRODUCTS -> ResponseEntity(canceledOrder.body as OrderDTO, HttpStatus.CONFLICT)
                ResponseType.CANNOT_RESTORE_MONEY -> ResponseEntity(canceledOrder.body as OrderDTO, HttpStatus.PAYMENT_REQUIRED)
                else -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: IllegalArgumentException) {
            println("OrderController.getOrder: The order $orderID cannot be found because the id is not in a valid format.")
            return ResponseEntity(null, HttpStatus.NOT_FOUND)
        }
    }
}