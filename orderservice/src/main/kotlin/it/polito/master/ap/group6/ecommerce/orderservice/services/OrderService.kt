package it.polito.master.ap.group6.ecommerce.orderservice.services

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO
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
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.*

interface OrderService {
    fun createOrder(placedOrder: PlacedOrderDTO): OrderDTO?
    fun getOrder(orderID: ObjectId): List<OrderDTO>?
    fun getOrdersByUser(userID: String): List<List<OrderDTO>>?
    fun cancelOrder(orderID: ObjectId): OrderDTO

    fun checkWallet(order: Order): String?
    fun completeTransaction(transactionId: String, order: Order): String?
    fun submitOrder(order: Order): Boolean
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
    @Autowired private val deliveryService: DeliveryService
) : OrderService {

    /**
     * Create an order inserting it into the database. The order will have a FAILED status if the user hasn't enough
     * money or if the warehouse hasn't enough products. Otherwise, the order will have a PAID status. For the inserted order
     * will be created a number of deliveries and inserted in the Delivery database. Each delivery is referred to a subset of products
     * of the order, which come from the same warehouse.
     * @param placedOrder, the order placed by the CatalogueService
     * @return an Order instance. It will have as status PAID if it is submitted successfully, FAILED otherwise
     */
    override fun createOrder(placedOrder: PlacedOrderDTO): OrderDTO? {
        //Create the order in PENDING status (no checks have been performed yet)
        var order = placedOrder.toModel()
        order.status = OrderStatus.PENDING
        order = orderRepository.save(order) //reassign because save gives to order an id

        //STEP 1: check if there are enough money and lock them on the user wallet
        val transactionId = checkWallet(order) ?: run {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order.toDto() //return if there aren't enough money or the wallet service is down
        }

        //STEP 2: if there are enough money, submit the order and create the needed deliveries in PENDING status
        val orderSubmitted = submitOrder(order)
        if (!orderSubmitted) {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order.toDto() //Return if there aren't enough products or the warehouse service is down
        }

        //STEP 3: complete the transaction if the delivery has started and mark order as paid. It also takes
        //care of canceling all the pending deliveries created in the STEP 2.
        completeTransaction(transactionId, order) ?: run {
            order.status = OrderStatus.FAILED
            orderRepository.save(order)
            return order.toDto() //return if the transaction fails or the wallet service is down
        }

        order.status = OrderStatus.PAID
        order = orderRepository.save(order)

        /**
         * Start the coroutine handling the delivery simulation.
         * If the order get CANCELED in the PAID status, the coroutine CANCEL all the associated deliveries.
         */
        deliveryService.startDeliveries(order.id.toString())

        return order.toDto()
    }

    /**
     * @param orderID, the id of the desired order
     * @return for a given orderId, returns a list of orders corresponding to all the deliveries associated to
     * that order. In this way we have a more fine-grained control of the status of each delivery in the order.
     */
    override fun getOrder(orderID: ObjectId): List<OrderDTO>? {
        val deliveries = deliveryRepository.findByOrderID(orderID.toString())
        val orderOptional = orderRepository.findById(orderID)
        if (orderOptional.isEmpty) return null //if the order doesn't exist, return null

        val order = orderOptional.get()
        //TODO: change the return type of find delivery from List<Optional<Delivery>> to Optional<List<Delivery>>
        if (deliveries.isEmpty()) return listOf(order.toDto()) //if there aren't deliveries associated to that order, return the order directly.
        val orderList = mutableListOf<Order>()
        deliveries.forEach {
            orderList.add(
                Order(
                    order.buyer,
                    it.get().products,
                    Utility.getResultingStatus(order.status!!, it.get().status!!),
                    it.get().shippingAddress
                )
            )
        }
        orderList.forEach { it.id = order.id }
        return orderList.map { it.toDto() }
    }

    /**
     * For a given user, getOrdersByUser returns all the orders associated to that user.
     * @param userID, the id of the user.
     * @return for each order, it returns a list of each delivery associated to that order with the corresponding status.
     */
    override fun getOrdersByUser(userID: String): List<List<OrderDTO>>? {
        val ordersOptional = orderRepository.findByBuyerId(userID)
        if (ordersOptional.isEmpty) return null //if the user doesn't exists, return null

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
                        order.buyer,
                        it.get().products,
                        Utility.getResultingStatus(order.status!!, it.get().status!!),
                        it.get().shippingAddress
                    )
                )
            }
            orderList.forEach { it.id = order.id }
            ordersList.add(orderList)
        }
        return ordersList.map { list -> list.map { it.toDto() } }
    }

    //TODO: how to ensure transactional operations? cannot update order and deliveries in two distinct moment.
    override fun cancelOrder(orderID: ObjectId): OrderDTO {
        val warehouse: String = "localhost:8084"
        val restTemplate = RestTemplate()

        val orderOptional = orderRepository.findById(orderID)
        if (orderOptional.isEmpty) {
            return orderOptional.get().toDto()
        }
        val order = orderOptional.get()
        if (order.status == OrderStatus.PAID || order.status == OrderStatus.PENDING) {
            //TODO: if it was PENDING, we must inform to NOT perform the recharge!!!
            order.status = OrderStatus.CANCELED
            orderRepository.save(order)
            //Cascade update on the deliveries associated to this order. Note that since the order is in PAID
            //or PENDING status, the associated delivery must be in PENDING status.
            val deliveries = deliveryRepository.findByOrderID(order.id.toString())
            if (deliveries.isEmpty()) return order.toDto() //no deliveries associated to this order yet. This means the order is in PENDING.
            for (delivery in deliveries) {
                delivery.get().status = DeliveryStatus.CANCELED
                deliveryRepository.save(delivery.get())
            }
            try {
                //Inform the warehouse that the delivery is canceled, so products must be restored
                val result: Unit = RestTemplate().delete(
                    "http://${warehouse}/warehouse/orders",
                    DeliveryListDTO(order.id, deliveries.map { it.get().toDto() })
                )
            } catch (e: Exception) {
                println("OrderService.cancelOrder: [${e.cause}]. Cannot contact the warehouse service to restore the products.")
                //TODO: need to rollback or inform the warehouse to delete those products!
            }

            println("OrderService.cancelOrder: Order ${order.id} canceled!")
        } else {
            println("OrderService.cancelOrder: Cannot cancel the order ${order.id}!")
        }
        return order.toDto()
    }

    /**
     * Check if the transaction can be performed, i.e. the user has enough money. The amount will be "Locked" but not
     * yet taken from the user's wallet.
     * @param order, the order corresponding to the transaction that will be created.
     * @return the Id of the transaction created by WalletService, null if the transaction could not be created or
     * there aren't enough money.
     */
    override fun checkWallet(order: Order): String? {
        val wallet: String = "localhost:8083"
        val restTemplate = RestTemplate()
        var transactionId: String?
        val transaction =
            TransactionDTO(order.buyer, order.price, Date(), "Order ${order.id}", TransactionStatus.PENDING)

        try {
            transactionId = restTemplate.postForObject(
                "http://${wallet}/wallet/checkavailability/${order.buyer?.id}", //"https://api.mocki.io/v1/f4359b2e"
                transaction, String::class.java
            )
            //if (transactionId != null)
                //transactionId = Gson().fromJson(transactionId, Properties::class.java).getProperty("transactionId")
        } catch (e: Exception) {
            println("OrderService.checkWallet: [${e.cause}]. Cannot contact the wallet service to check the availability.")
            return null
        }

        if (transactionId != null) {
            println("OrderService.checkWallet: ${transaction.amount} $ locked on the user ${order.buyer?.id}' s wallet.")
            return transactionId.toString()
        } else {
            println("OrderService.checkWallet: The user ${order.buyer?.id}  has not enough money to purchase the order.")
            return null
        }
    }

    /**
     * The warehouse service is reached out to verify if there are enough products to satisfy the order.
     * In this case, a list of deliveries for each warehouse from which the products are coming is retrieved,
     * and all the deliveries are saved in the database with a PENDING status.
     * @param order the order that has been requested by catalog service.
     * @return boolean indicating whether there are enough products and the process can continue, or not.
     */
    override fun submitOrder(order: Order): Boolean {
        var warehouse: String = "localhost:8084"
        val restTemplate = RestTemplate()
        val deliveryList: DeliveryListDTO?
        try {
            deliveryList = restTemplate.postForObject<DeliveryListDTO>(
                "http://${warehouse}/warehouse/orders", //"https://api.mocki.io/v1/6ace7eb0",
                order.toDto(), DeliveryListDTO::class.java
            )
        } catch (e: Exception) {
            println("OrderService.submitOrder: [${e.cause}]. Cannot contact the warehouse service to retrieve the products.")
            return false
            //TODO: Specify that we don't know if there are or not products.
        }

        if (deliveryList == null) {
            println("OrderService.submitOrder: One or more products are not available in the warehouses.")
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
            println("OrderService.submitOrder: ${deliveryList.deliveryList!!.size} deliveries have been scheduled for the order ${order.id}.")
            return true
        }
    }

    /**
     * The wallet service is reached out again to complete the transaction started in the STEP 1.
     * Notice that if this step goes wrong, the method is in charge of CANCELING all the deliveries in the PENDING
     * status created in the STEP 2 and inform the warehouse service to restore the products.
     * @param transactionId, the id of the transaction to be completed
     * @param order, the order associated to the transaction performed
     * @return the id of the transaction if it is correctly completed, null otherwise.
     */
    override fun completeTransaction(transactionId: String, order: Order): String? {
        val wallet: String = "localhost:8083"
        val warehouse: String = "localhost:8084"
        val restTemplate = RestTemplate()
        var transactionRes: String?

        val transaction =
            TransactionDTO(order.buyer, order.price, Date(), "Order ${order.id}", TransactionStatus.ACCEPTED)

        try {
            transactionRes = restTemplate.postForObject(
                "http://${wallet}/wallet/performtransaction/$transactionId", //"https://api.mocki.io/v1/f4359b2e",
                transaction, String::class.java
            )
            //transactionRes = Gson().fromJson(transactionRes, Properties::class.java).getProperty("transactionId")
        } catch (e: Exception) {
            println("OrderService.completeTransaction: [${e.cause}]. Cannot contact the wallet service to complete the transaction.")
            transactionRes = null
        }
        if (transactionRes != null) {
            println("OrderService.completeTransaction: The order ${order.id} is confirmed.")
        } else {
            println("OrderService.completeTransaction: An error occurred while confirming the transaction ${transactionId}, the order ${order.id} is failed.")
            //Set to CANCELED all the associated deliveries scheduled in the STEP 2
            val deliveries = deliveryRepository.findByOrderID(order.id!!)
            deliveries.all { it.get().status == DeliveryStatus.CANCELED }
            println("OrderService.completeTransaction: All deliveries associated with ${order.id} have been canceled.")
            try {
                //Inform the warehouse that the delivery is canceled, so products must be restored
                val result: Unit = RestTemplate().delete(
                    "http://${warehouse}/warehouse/orders",
                    DeliveryListDTO(order.id, deliveries.map { it.get().toDto() })
                )
            } catch (e: Exception) {
                println("OrderService.completeTransaction: [${e.cause}]. Cannot contact the warehouse service to restore the products.")
                //TODO: need to rollback or inform the warehouse to delete those products!
            }
        }
        return transactionRes
    }
}