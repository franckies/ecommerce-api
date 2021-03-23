package it.polito.master.ap.group6.ecommerce.orderservice.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.TransactionDTO
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.common.misc.TransactionStatus
import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.Utility
import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.*

interface OrderService {
    fun createOrder(placedOrder: PlacedOrderDTO): Order?
    fun getOrder(orderID: ObjectId): List<Order>?
    fun getOrdersByUser(userID: String): List<List<Order>>?
    fun cancelOrder(orderID: ObjectId): Optional<Order>

    fun checkWallet(order: Order): String?
    fun completeTransaction(transactionId: String, order: Order): String?
    fun submitOrder(order: Order): Boolean
}

/**
 * The order service. Implements the business logic.
 * @param orderRepository a reference to the Repository handling the database interaction.
 * @author Francesco Semeraro
 */
@Service
@Transactional
class OrderServiceImpl(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
) : OrderService {


    /**
     * Create an order inserting it into the database. The order will have a FAILED status if the user hasn't enough
     * money or if the warehouse hasn't enough products. Otherwise, the order will have a PAID status. For the inserted order
     * will be created a number of deliveries and inserted in the Delivery database. Each delivery is referred to a subset of products
     * of the order, which come from the same warehouse.
     * @param placedOrder, the order placed by the CatalogueService
     * @return an Order instance. It will have as status PAID if it is submitted successfully, FAILED otherwise
     */
    override fun createOrder(placedOrder: PlacedOrderDTO): Order? {
        //Create the order in PENDING status (no checks have been performed yet)
        var order = placedOrder.toModel()
        order.status = OrderStatus.PENDING
        order = orderRepository.save(order) //reassign because save gives to order an id

        //STEP 1: check if there are enough money and lock them on the user wallet
        val transactionId = checkWallet(order) ?: run {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order //return if there aren't enough money or the wallet service is down
        }

        //STEP 2: if there are enough money, submit the order
        val orderSubmitted = submitOrder(order)
        if (!orderSubmitted) {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order //Return if there aren't enough products or the warehouse service is down
        }

        //STEP 3: complete the transaction if the delivery has started and mark order as paid
        val transactionResult = completeTransaction(transactionId, order) ?: run {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order //return if the transaction fails or the wallet service is down
        }

        order.status = OrderStatus.PAID
        order = orderRepository.save(order)
        //TODO: if at least one delivery associated to an order is in DELIVERING state, then the order is in DELIVERING and cannot be canceled.
        return order
    }

    /**
     * For a given orderId, getOrder returns a list of orders corresponding to all the deliveries associated to
     * that order. In this way we have a more fine-grained control of the status of each delivery in the order.
     * @param orderID, the id of the desired order
     */
    override fun getOrder(orderID: ObjectId): List<Order>? {
        val deliveries = deliveryRepository.findByOrderID(orderID.toString())
        val order = orderRepository.findById(orderID)
        if (order.isEmpty) return null //if the order doesn't exist, return null
        if (deliveries.isEmpty()) return listOf(order.get()) //if there aren't deliveries associated to that order, return the order directly.
        val orderList = mutableListOf<Order>()
        deliveries.forEach {
            orderList.add(
                Order(
                    order.get().buyer,
                    it.get().products,
                    Utility.getResultingStatus(order?.get().status!!, it.get().status!!),
                    it.get().shippingAddress
                )
            )
        }
        orderList.forEach { it.id = order.get().id }
        return orderList
    }

    /**
     * For a given user, getOrdersByUser returns all the orders associated to that user.
     * For each order, it returns a list of each delivery associated to that order with the corresponding status.
     * @param userID, the id of the user.
     */
    override fun getOrdersByUser(userID: String): List<List<Order>>? {
        val orders = orderRepository.findByBuyerId(userID)
        if (orders.isEmpty()) return null //if the user doesn't exists, return null
        val ordersList = mutableListOf<List<Order>>()
        for (order in orders) {
            val deliveries = deliveryRepository.findByOrderID(order.get().id.toString())
            if (deliveries.isEmpty()) {
                ordersList.add(listOf(order.get()))
                continue //if there aren't deliveries associated to this order, add the order with its status(PENDING or FAILED)
            }
            val orderList = mutableListOf<Order>()
            deliveries.forEach {
                orderList.add(
                    Order(
                        order.get().buyer,
                        it.get().products,
                        Utility.getResultingStatus(order?.get().status!!, it.get().status!!),
                        it.get().shippingAddress
                    )
                )
            }
            orderList.forEach { it.id = order.get().id }
            ordersList.add(orderList)
        }
        return ordersList
    }

    //TODO: how to ensure transactional operations? cannot update order and deliveries in two distinct moment.
    override fun cancelOrder(orderID: ObjectId): Optional<Order> {
        var warehouse: String = "localhost:8084"
        val restTemplate = RestTemplate()

        val order = orderRepository.findById(orderID)
        if (order.isEmpty) {
            return order
        }
        if (order.get().status == OrderStatus.PAID || order.get().status == OrderStatus.PENDING) {
            order.get().status = OrderStatus.CANCELED
            orderRepository.save(order.get())
            //Cascade update on the deliveries associated to this order. Note that since the order is in PAID
            //or PENDING status, the associated delivery must be in PENDING status.
            val deliveries = deliveryRepository.findByOrderID(order.get().id.toString())
            if (deliveries.isEmpty()) return order //no deliveries associated to this order yet. This means the order is in PENDING.
            for (delivery in deliveries) {
                delivery.get().status = DeliveryStatus.CANCELED
                deliveryRepository.save(delivery.get())
            }
            try {
                //Inform the warehouse that the delivery is canceled, so products must be restored
                val result: Unit = RestTemplate().delete(
                    "http://${warehouse}/warehouse/orders",
                    DeliveryListDTO(order.get().id, deliveries.map { it.get().toDto() })
                )
            } catch(e: Exception){
                println("OrderService: $e, cannot contact the warehouse service.")
                //TODO: need to rollback or inform the warehouse to delete those products!
            }

            println("OrderService: Order ${order.get().id} canceled!")
        } else {
            println("OrderService: Cannot cancel the order ${order.get().id}!")
        }
        return order
    }

    /**
     * Check if the transaction can be performed, i.e. the user has enough money. The amount will be "Locked" but not
     * yet taken from the user's wallet.
     * @param order, the order corresponding to the transaction that will be created.
     * @return the Id of the transaction created by WalletService
     */
    override fun checkWallet(order: Order): String? {
        val wallet: String = "localhost:8083"
        val restTemplate = RestTemplate()
        var transactionId: String? = null
        val transaction =
            TransactionDTO(order.buyer, order.price, Date(), "Order ${order.id}", TransactionStatus.PENDING)

        try{
            transactionId = restTemplate.postForObject(
                "http://${wallet}/wallet/checkavailability/${order.buyer?.id}",
                transaction, String::class.java
            )
            transactionId = Gson().fromJson(transactionId, Properties::class.java).getProperty("transactionId")
        } catch( e: Exception){
            println("OrderService: $e, cannot contact the wallet service.")
            return null
        }

        if (transactionId != null) {
            println("${transaction.amount} $ locked on the user ${order.buyer?.id}' s wallet")
            return transactionId.toString()
        } else {
            println("The user ${order.buyer?.id}  has not enough money to purchase the order.")
            return null
        }
    }

    override fun submitOrder(order: Order): Boolean {
        var warehouse: String = "localhost:8084"
        val restTemplate = RestTemplate()
        val deliveryList: DeliveryListDTO?
        try{
            deliveryList = restTemplate.postForObject<DeliveryListDTO>(
                "http://${warehouse}/warehouse/orders",
                order.toDto(), DeliveryListDTO::class.java
            )
        } catch (e: Exception){
            println("OrderService: $e, cannot contact the warehouse service.")
            return false
            //TODO: Specify that we don't know if there are or not products.
        }

        if (deliveryList == null) {
            println("One or more products are not available in the warehouses.")
            return false
        } else {
            for (delivery in deliveryList.deliveryList!!) {
                //save each delivery in the database with a PENDING status
                deliveryRepository.save(
                    Delivery(
                        order.id,
                        order.deliveryAddress,
                        delivery.warehouse,
                        delivery.delivery?.map { it.toModel() },
                        DeliveryStatus.PENDING
                    )
                )
            }
            //After a while update randomly a delivery status for testing
            GlobalScope.launch { // launch a new coroutine in background and continue
                while (true) {
                    delay(100000L)
                    val deliveries = deliveryRepository.findByOrderID(order.id!!)
                    if (deliveries.all { it.get().status == DeliveryStatus.DELIVERING }) {
                        println("All deliveries associated to ${order.id} have been shipped.")
                        break
                    }
                    val randomDelivery = deliveries.get(Random().nextInt(deliveries.size))
                    if (randomDelivery.get().status == DeliveryStatus.PENDING) randomDelivery.get().status =
                        DeliveryStatus.DELIVERING
                    deliveryRepository.save(randomDelivery.get())

                    //CONSEQUENTLY UPDATE THE ORDER
                    if (order.status == OrderStatus.PAID) {
                        order.status = OrderStatus.DELIVERING
                        orderRepository.save(order)
                    }
                }
            }
            return true
        }
    }

    override fun completeTransaction(transactionId: String, order: Order): String? {
        val wallet: String = "localhost:8083"
        val restTemplate = RestTemplate()
        var transactionResult: String?

        val transaction =
            TransactionDTO(order.buyer, order.price, Date(), "Order ${order.id}", TransactionStatus.ACCEPTED)

        try {
            transactionResult = restTemplate.postForObject(
                "http://${wallet}/wallet/performtransaction/$transactionId",
                transaction, String::class.java
            )
        } catch(e: Exception){
            println("OrderService: $e, cannot contact the wallet service.")
            return null
        }
        return transactionResult
    }
}