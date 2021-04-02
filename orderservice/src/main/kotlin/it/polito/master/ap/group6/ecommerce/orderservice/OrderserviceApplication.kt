package it.polito.master.ap.group6.ecommerce.orderservice

import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderLoggerRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import it.polito.master.ap.group6.ecommerce.orderservice.services.OrderServiceAsync
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.core.KafkaTemplate
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class OrderserviceApplication(
    @Autowired orderRepo: OrderRepository,
    @Autowired deliveryRepo: DeliveryRepository,
    @Autowired loggerRepo: OrderLoggerRepository,
    @Autowired orderServiceAsync: OrderServiceAsync,
    val kafkaTemplate: KafkaTemplate<String, String>
) {
    init {
        //clear table
        orderRepo.deleteAll()
        deliveryRepo.deleteAll()
        loggerRepo.deleteAll()

//        //ONLY NEEDED IN ASYNC MODE=====================================================================================
//        //As soon as order service wakes up, make a consistency control on the logger and rollback inconsistent status
//        val loggerList = loggerRepo.findAll()
//        for (orderLog in loggerList) {
//            when (orderLog.orderStatus) {
//                OrderLoggerStatus.PENDING -> {
//                    println("OrderServiceApplication.init: inconsistent order ${orderLog.orderID} found.")
//                    orderServiceAsync.failOrder(orderLog.orderID!!)
//                    loggerRepo.deleteById(ObjectId(orderLog.orderID))
//                    kafkaTemplate.send("rollback", orderLog.orderID)
//                }
//                OrderLoggerStatus.TRANSACTION_OK -> {
//                    println("OrderServiceApplication.init: inconsistent order ${orderLog.orderID} found.")
//                    orderServiceAsync.failOrder(orderLog.orderID!!)
//                    loggerRepo.deleteById(ObjectId(orderLog.orderID))
//                    kafkaTemplate.send("rollback", orderLog.orderID)
//                }
//                OrderLoggerStatus.DELIVERY_OK -> {
//                    println("OrderServiceApplication.init: inconsistent order ${orderLog.orderID} found.")
//                    orderServiceAsync.failOrder(orderLog.orderID!!)
//                    loggerRepo.deleteById(ObjectId(orderLog.orderID))
//                    kafkaTemplate.send("rollback", orderLog.orderID)
//                }
//                OrderLoggerStatus.PAID -> {
//                    println("OrderServiceApplication.init: found order ${orderLog.orderID} paid.")
//                }
//                OrderLoggerStatus.DELIVERING -> {
//                    println("OrderServiceApplication.init: found order ${orderLog.orderID} delivering.")
//                }
//            }
//        }


//        //Populate Order
//        val userDTOList = mutableListOf<UserDTO>().apply {
//            add(UserDTO("1239820421", "Francesco", "Semeraro", "Milano", "User", UserRole.CUSTOMER.toString()))
//            add(UserDTO("2142109842", "Nicolò", "Chiapello", "Torino", "User", UserRole.CUSTOMER.toString()))
//        }
//        val productList = mutableListOf<ProductDTO>().apply {
//            add(ProductDTO("sf5321", "Umbrella", "Repairs from rain", "Misc", "umbrella_pic", 20f))
//            add(ProductDTO("sf1221", "Shoes", "Black shoes", "Dressing", "shoes_pic", 50f))
//            add(ProductDTO("sf13321","Tablet", "iPad 2018", "Electronics", "tablet_pic", 300f))
//        }
//        val purchaseList1 = mutableListOf<Purchase>().apply {
//            add(Purchase(productList[0].id!!, 2, 15f))
//            add(Purchase(productList[2].id!!, 1, 320f))
//        }
//        val purchaseList2 = mutableListOf<Purchase>().apply {
//            add(Purchase(productList[0].id!!, 1, 21f))
//            add(Purchase(productList[1].id!!, 1, 47f))
//        }
//
//        val orderList = mutableListOf<Order>().apply {
//            add(Order(userDTOList[0].id, purchaseList1, OrderStatus.PAID, "Milan"))
//            add(Order(userDTOList[1].id, purchaseList2, OrderStatus.DELIVERING, "Turin"))
//        }
//        orderRepo.saveAll(orderList)
//
//        //test order POST
//        val o = PlacedOrderDTO(ObjectId.get().toString(), "2142109842", purchaseList1.map { it.toDto() }, "Turin")
//        val jsonString = Gson().toJson(o)
//        println(jsonString)
//
//        //Populate delivery
//        //umbrella is shipped from asti, while tablet from Genova.
//        //Only the first user has associated deliveries, the second user order is still in pending.
//        val deliveryList = mutableListOf<Delivery>().apply{
//            add(Delivery(orderList[0].id, "Milan","Amazon Asti", listOf(purchaseList1[0]), DeliveryStatus.PENDING))
//            add(Delivery(orderList[0].id, "Milan", "Amazon Genova", listOf(purchaseList1[1]), DeliveryStatus.PENDING))
//            //add(Delivery(orderList[1].id, "Turin", WarehouseDTO("Ebay", "Roma"), listOf(purchaseList2[0]), DeliveryStatus.DELIVERING))
//            //add(Delivery(orderList[1].id, "Turin", WarehouseDTO("Ebay", "Bari"), listOf(purchaseList2[1]), DeliveryStatus.PENDING))
//        }
//
//        val testDeliveryList = mutableListOf<Delivery>().apply{
//            add(Delivery(o.toModel().id, "Turin", "Amazon Roma", listOf(purchaseList1[0]), DeliveryStatus.PENDING))
//            add(Delivery(o.toModel().id, "Turin", "Amazon Lecce", listOf(purchaseList1[1]), DeliveryStatus.PENDING))
//        }
//        val d = DeliveryListDTO(o.toModel().id, testDeliveryList.map { it.toDto() })
//        val jsonStringd = Gson().toJson(d)
//        println(jsonStringd)
//        deliveryRepo.saveAll(deliveryList)
//
    }
}


fun main(args: Array<String>) {
    runApplication<OrderserviceApplication>(*args)
}
