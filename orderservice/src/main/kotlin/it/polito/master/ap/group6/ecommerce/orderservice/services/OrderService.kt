package it.polito.master.ap.group6.ecommerce.orderservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Response
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.ResponseType
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Utility
import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.OrderLogger
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderLoggerRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*

interface OrderService {
    fun createOrder(placedOrder: PlacedOrderDTO): Response
    fun getOrder(orderID: ObjectId): Response
    fun getOrdersByUser(userID: String): Response
    fun cancelOrder(orderID: ObjectId): Response

    fun checkWallet(orderID: ObjectId): Response
    fun completeTransaction(transactionId: String, orderID: ObjectId): Response
    fun submitOrder(orderID: ObjectId): Response
    fun undoDeliveries(orderID: ObjectId): Response
    fun undoTransaction(orderID: ObjectId): Response
}

/**
 * The order service. Implements the business logic.
 * @param orderRepository a reference to the Repository handling the database interaction for orders.
 * @param deliveryRepository a reference to the Repository handling the database interaction for deliveries.
 * @param deliveryService the service simulating the shipping of the orders.
 * @author Francesco Semeraro
 */
@Service
@Transactional
class OrderServiceImpl(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val deliveryService: DeliveryService,
    @Autowired private val orderLoggerRepository: OrderLoggerRepository
) : OrderService {

    /**
     * Create an order inserting it into the database. The order will have a FAILED status if the user hasn't enough
     * money or if the warehouse hasn't enough products. Otherwise, the order will have a PAID status. For the inserted order
     * will be created a number of deliveries and inserted in the Delivery database. Each delivery is referred to a subset of products
     * of the order, which come from the same warehouse.
     * @param placedOrder, the order placed by the CatalogueService
     * @return a Response instance having a status code corresponding to the event that occurred. The
     *  response will have as body an Order instance having as status PAID if it is submitted successfully, FAILED otherwise
     */
    override fun createOrder(placedOrder: PlacedOrderDTO): Response {
        //Create the order in PENDING status (no checks have been performed yet)
        //The order ID coincides with the saga id which is given by the catalog service
        var order = placedOrder.toModel()
        order.status = OrderStatus.PENDING
        order = orderRepository.save(order)
        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.PENDING, Date()))

        //STEP 1: check if there are enough money and lock them on the user wallet
        val step1 = checkWallet(ObjectId(order.id))
        if (step1.responseId != ResponseType.MONEY_LOCKED) {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            val res = Response.notEnoughMoney()
            res.body = order.toDto()
            orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.CHECK_WALLET_FAILED, Date()))
            return res
        }
        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.CHECK_WALLET_COMPLETED, Date()))
        //STEP 2: if there are enough money, submit the order and create the needed deliveries in PENDING status
        val step2 = submitOrder(ObjectId(order.id))
        if (step2.responseId != ResponseType.ORDER_SUBMITTED) {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            val res = Response.productNotAvailable()
            res.body = order.toDto()
            orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.SUBMIT_ORDER_FAILED, Date()))
            return res
            //return order.toDto() //Return if there aren't enough products or the warehouse service is down
        }
        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.SUBMIT_ORDER_COMPLETED, Date()))
        //STEP 3: complete the transaction if the delivery has started and mark order as paid. It also takes
        //care of canceling all the pending deliveries created in the STEP 2.
        val step3 = completeTransaction(step1.body as String, ObjectId(order.id))
        if (step3.responseId != ResponseType.ORDER_CONFIRMED) {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            val res = Response.notEnoughMoney()
            res.body = order.toDto()
            orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.COMPLETE_TRANSACTION_FAILED, Date()))
            return res
        }
        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.COMPLETE_TRANSACTION_COMPLETED, Date()))
        order.status = OrderStatus.PAID
        order = orderRepository.save(order)

        orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.PAID, Date()))
        /**
         * Start the coroutine handling the delivery simulation.
         * If the order get CANCELED in the PAID status, the coroutine CANCEL all the associated deliveries.
         */
        deliveryService.startDeliveries(order.id.toString())

        val res = Response.orderCreated()
        res.body = order.toDto()
        return res
    }

    /**
     * @param orderID, the id of the desired order
     * @return a Response instance having a status code corresponding to the event that occurred. If all go right,
     * the response will have as body an object containing for a given orderId,a list of orders corresponding to
     * all the deliveries associated to that order. In this way we have a more fine-grained control of the status
     * of each delivery in the order.
     */
    override fun getOrder(orderID: ObjectId): Response {
        val deliveries = deliveryRepository.findByOrderID(orderID.toString())
        val orderOptional = orderRepository.findById(orderID)
        if (orderOptional.isEmpty) {
            println("OrderService.getOrder: the order $orderID cannot be found.")
            return Response.orderCannotBeFound() //if the order doesn't exist, return
        }

        val order = orderOptional.get()
        if (deliveries.isEmpty()) {
            val res = Response.orderFound()
            res.body = listOf(order.toDto())
            return res //if there aren't deliveries associated to that order, return the order directly.
        }
        val orderList = mutableListOf<Order>()
        deliveries.forEach {
            orderList.add(
                Order(
                    order.buyerId,
                    it.get().products,
                    Utility.getResultingStatus(order.status!!, it.get().status!!),
                    it.get().shippingAddress
                )
            )
        }
        orderList.forEach { it.id = order.id }
        val res = Response.orderFound()
        res.body = orderList.map { it.toDto() }
        return res
    }

    /**
     * For a given user, getOrdersByUser returns all the orders associated to that user.
     * @param userID, the id of the user.
     * @return a Response instance having a status code corresponding to the event that occurred. If all go right
     * the response will have as body an object containing for each order,
     * a list of each delivery associated to that order with the corresponding status.
     */
    override fun getOrdersByUser(userID: String): Response {
        val ordersOptional = orderRepository.findByBuyerId(userID)
        if (ordersOptional.isEmpty) {
            return Response.orderCannotBeFound()
        } //if the user doesn't exists, return null

        val orders = ordersOptional.get()
        val ordersList = mutableListOf<List<Order>>()
        for (order in orders) {
            val deliveries = deliveryRepository.findByOrderID(order.id.toString())
            if (deliveries.isEmpty()) {
                ordersList.add(listOf(order))
                continue //if there aren't deliveries associated to this order, add the order with its status(PENDING or FAILED)
            }
            val orderList = mutableListOf<Order>()
            deliveries.forEach {
                orderList.add(
                    Order(
                        order.buyerId,
                        it.get().products,
                        Utility.getResultingStatus(order.status!!, it.get().status!!),
                        it.get().shippingAddress
                    )
                )
            }
            orderList.forEach { it.id = order.id }
            ordersList.add(orderList)
        }
        val res = Response.orderFound()
        res.body = ordersList.map { list -> list.map { it.toDto() } }
        return res
    }

    override fun cancelOrder(orderID: ObjectId): Response {
        val orderOptional = orderRepository.findById(orderID)
        if (orderOptional.isEmpty) {
            println("OrderService.cancelOrder: The order $orderID cannot be found.")
            return Response.orderCannotBeFound()
        }
        val order = orderOptional.get()
        if (order.status == OrderStatus.PAID) {
            order.status = OrderStatus.CANCELED
            orderLoggerRepository.save(OrderLogger(order.id, OrderLoggerStatus.CANCELED, Date()))
            orderRepository.save(order)
            //Cascade update on the deliveries associated to this order and undone of the transaction
            undoDeliveries(ObjectId(order.id))
            undoTransaction(ObjectId(order.id))
            println("OrderService.cancelOrder: Order ${order.id} canceled!")
        } else {
            println("OrderService.cancelOrder: Cannot cancel the order ${order.id}!")
        }
        val res = Response.orderFound()
        res.body = order.toDto()
        return res
    }

    /**
     * Check if the transaction can be performed, i.e. the user has enough money. The amount will be "Locked" but not
     * yet taken from the user's wallet.
     * @param orderID, the order corresponding to the transaction that will be created.
     * @return a Response instance having a status code corresponding to the event that occurred.
     */
    override fun checkWallet(orderID: ObjectId): Response {
        val order: Order = orderRepository.findById(orderID).get()
        val wallet = "localhost:8083"
        val restTemplate = RestTemplate()
        val transactionId: String?
        val transaction =
            TransactionDTO(order.id, order.buyerId, order.price, Date(), TransactionStatus.PENDING)

        try {
            transactionId = restTemplate.postForObject(
                "http://${wallet}/wallet/checkavailability/${order.buyerId}", //"https://api.mocki.io/v1/f4359b2e"
                transaction, String::class.java
            )
        } catch (e: HttpClientErrorException) {
            return when (e.statusCode) {
                HttpStatus.CONFLICT -> {
                    println("OrderService.checkWallet: The user ${order.buyerId}  has not enough money to purchase the order.")
                    Response.notEnoughMoney()
                }
                HttpStatus.NOT_FOUND -> {
                    println("OrderService.checkWallet: the user ${order.buyerId} has not a created wallet.")
                    Response.walletNotFound()
                }
                else -> {
                    println("OrderService.checkWallet: An unknown error occurred.")
                    Response.cannotReachTheMS()
                }
            }
        } catch (e: ResourceAccessException) {
            println("OrderService.checkWallet: [${e.cause}]. Cannot contact the wallet service to check the availability.")
            return Response.cannotReachTheMS()
        } catch (e: Exception) {
            println("OrderService.checkWallet: [${e.cause}]. An unknown error occurred.")
            return Response.cannotReachTheMS()
        }

        println("OrderService.checkWallet: ${transaction.amount} $ locked on the user ${order.buyerId}' s wallet.")
        val res = Response.moneyLocked()
        res.body = transactionId
        return res
    }

    /**
     * The warehouse service is reached out to verify if there are enough products to satisfy the order.
     * In this case, a list of deliveries for each warehouse from which the products are coming is retrieved,
     * and all the deliveries are saved in the database with a PENDING status. If there aren't enough products
     * or the warehouse is not responding, the wallet is informed to restore the blocked transaction
     * @param orderID the order that has been requested by catalog service.
     * @return a Response instance having a status code corresponding to the event that occurred.
     */
    override fun submitOrder(orderID: ObjectId): Response {
        val order: Order = orderRepository.findById(orderID).get()
        val warehouse = "localhost:8084"

        val deliveryList: DeliveryListDTO?
        try {
            deliveryList = RestTemplate().postForObject(
                "http://${warehouse}/warehouse/orders", //"https://api.mocki.io/v1/6ace7eb0",
                order.toDto(), DeliveryListDTO::class.java
            )
        } catch (e: HttpClientErrorException) {
            return when (e.statusCode) {
                HttpStatus.CONFLICT -> {
                    println("OrderService.submitOrder: One or more products are not available in the warehouses.")
                    undoTransaction(ObjectId(order.id)) //Tell wallet to restore the transaction
                    Response.productNotAvailable()
                }
                HttpStatus.NOT_FOUND ->{
                    println("OrderService.submitOrder: One or more products do not exist in the database.")
                    undoTransaction(ObjectId(order.id)) //Tell wallet to restore the transaction
                    Response.productNotAvailable()
                }
                else -> {
                    println("OrderService.submitOrder: An unknown error occurred.")
                    undoTransaction(ObjectId(order.id)) //Tell wallet to restore the transaction
                    Response.cannotReachTheMS()
                }
            }
        } catch (e: ResourceAccessException) {
            println("OrderService.submitOrder: [${e.cause}]. Cannot contact the warehouse service to retrieve the products.")
            return Response.cannotReachTheMS()
        } catch (e: Exception) {
            println("OrderService.submitOrder: [${e.cause}]. An unknown error occurred.")
            return Response.cannotReachTheMS()
        }

        for (delivery in deliveryList!!.deliveryList!!) {
            //save each delivery in the database with a PENDING status
            deliveryRepository.save(
                Delivery(
                    order.id,
                    order.deliveryAddress,
                    delivery.warehouseID,
                    delivery.purchases?.map { it.toModel() },
                    DeliveryStatus.PENDING
                )
            )
        }
        println("OrderService.submitOrder: ${deliveryList.deliveryList!!.size} deliveries have been scheduled for the order ${order.id}.")
        return Response.orderSubmitted()
    }

    /**
     * The wallet service is reached out again to complete the transaction started in the STEP 1.
     * Notice that if this step goes wrong, the method is in charge of calling undoDeliveries(..) to
     * CANCEL all the deliveries in the PENDING status created in the STEP 2 and inform
     * the warehouse service to restore the products.
     * @param transactionId, the id of the transaction to be completed
     * @param orderID, the order associated to the transaction performed
     * @return a Response instance having a status code corresponding to the event that occurred.
     */
    override fun completeTransaction(transactionId: String, orderID: ObjectId): Response {
        val order: Order = orderRepository.findById(orderID).get()
        val wallet = "localhost:8083"
        val orderConfirmationID: String?

        try {
            orderConfirmationID = RestTemplate().getForObject(
                "http://${wallet}/wallet/performtransaction/${order.id}", //"https://api.mocki.io/v1/f4359b2e",
                String::class.java
            )
        } catch (e: HttpClientErrorException) {
            //if something goes wrong cancel also deliveries associated with the order
            undoDeliveries(ObjectId(order.id))
            return when (e.statusCode) {
                HttpStatus.CONFLICT -> {
                    println("OrderService.completeTransaction: The transaction $transactionId cannot be confirmed.")
                    Response.notEnoughMoney()
                }
                HttpStatus.NOT_FOUND -> {
                    println("OrderService.completeTransaction: the transaction $transactionId does not exist.")
                    Response.walletNotFound()
                }
                else -> {
                    println("OrderService.completeTransaction: An unknown error occurred.")
                    Response.cannotReachTheMS()
                }
            }
        } catch (e: ResourceAccessException) {
            //if something goes wrong cancel also deliveries associated with the order
            undoDeliveries(ObjectId(order.id))
            println("OrderService.completeTransaction: [${e.cause}]. Cannot contact the wallet service to perform the transaction.")
            return Response.cannotReachTheMS()
        } catch (e: Exception) {
            //if something goes wrong cancel also deliveries associated with the order
            undoDeliveries(ObjectId(order.id))
            println("OrderService.completeTransaction: [${e.cause}]. An unknown error occurred.")
            return Response.cannotReachTheMS()
        }

        println("OrderService.completeTransaction: The order ${order.id} is confirmed.")
        val res = Response.orderConfirmed()
        res.body = orderConfirmationID
        return res
    }

    override fun undoDeliveries(orderID: ObjectId): Response {
        val warehouse = "localhost:8084"
        val order = orderRepository.findById(orderID).get()
        var i: Int = 0
        //try to contact warehouse until the product is restored
        GlobalScope.launch {
            while (true) {
                delay(10000L)
                i++
                val deliveries = deliveryRepository.findByOrderID(order.id.toString())
                for (delivery in deliveries) {
                    delivery.get().status = DeliveryStatus.CANCELED
                    deliveryRepository.save(delivery.get())
                }
                try {
                    //Inform the warehouse that the delivery is canceled, so products must be restored
                    val result: String? = RestTemplate().getForObject(
                        "http://${warehouse}/warehouse/orders/restore/${order.id}",
                        String::class.java
                    )
                } catch (e: ResourceAccessException) {
                    println("OrderService.undoDeliveries: [${e.cause}]. Cannot contact the warehouse service to restore the products.")
                    val res = Response.cannotRestoreProducts()
                    res.body = order
                    continue
                } catch (e: Exception) {
                    println("OrderService.undoDeliveries: [${e.cause}]. An unknown error occurred.")
                    val res = Response.cannotRestoreProducts()
                    res.body = order
                    continue
                }
                println("OrderService.undoDeliveries: Products restored after $i tentatives.")
                break
            }
            this.cancel()
        }
        return Response.deliveriesUndone()
    }

    override fun undoTransaction(orderID: ObjectId): Response {
        val order = orderRepository.findById(orderID).get()

        val wallet = "localhost:8083"
        var res: Response
        var i: Int = 0
        //Try to contact the wallet until the transaction is restored
        GlobalScope.launch {
            while (true) {
                delay(10000L)
                i++
                try {
                    //Inform the wallet that the delivery is canceled, so must restore the money
                    val transactionId: String? = RestTemplate().getForObject(
                        "http://${wallet}/wallet/undo/${order.id}",
                        String::class.java
                    )
                } catch (e: HttpClientErrorException) {
                    when (e.statusCode) {
                        HttpStatus.NOT_FOUND -> res = Response.orderCannotBeFound()
                        else -> res = Response.cannotReachTheMS()
                    }
                    continue
                } catch (e: ResourceAccessException) {
                    println("OrderService.undoTransaction: [${e.cause}]. Cannot contact the wallet service to restore the money.")
                    res = Response.cannotRestoreMoney()
                    res.body = order
                    continue
                } catch (e: Exception) {
                    println("OrderService.undoTransaction: [${e.cause}]. An unknown error occurred.")
                    res = Response.cannotRestoreMoney()
                    res.body = order
                    continue
                }
                println("OrderService.undoTransaction: Transaction undone after $i tentatives.")
                break
            }
            this.cancel()
        }
        return Response.transactionUndone()
    }
}