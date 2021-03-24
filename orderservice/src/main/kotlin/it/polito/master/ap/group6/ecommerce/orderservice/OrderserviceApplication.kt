package it.polito.master.ap.group6.ecommerce.orderservice

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import it.polito.master.ap.group6.ecommerce.orderservice.models.Delivery
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.Purchase
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toModel
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.DeliveryRepository
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class OrderserviceApplication(
    @Autowired orderRepo: OrderRepository,
    @Autowired deliveryRepo: DeliveryRepository
) {
    init {
        //clear table
        /*orderRepo.deleteAll()
        deliveryRepo.deleteAll()

        //Populate Order
        val userDTOList = mutableListOf<UserDTO>().apply {
            add(UserDTO("1239820421", "Francesco", "Semeraro", "Milano", "User", UserRole.CUSTOMER.toString()))
            add(UserDTO("2142109842", "Nicolò", "Chiapello", "Torino", "User", UserRole.CUSTOMER.toString()))
        }
        val productList = mutableListOf<ProductDTO>().apply {
            add(ProductDTO("sf5321", "Umbrella", "Repairs from rain", "Misc", "umbrella_pic", 20f))
            add(ProductDTO("sf1221", "Shoes", "Black shoes", "Dressing", "shoes_pic", 50f))
            add(ProductDTO("sf13321","Tablet", "iPad 2018", "Electronics", "tablet_pic", 300f))
        }
        val purchaseList1 = mutableListOf<Purchase>().apply {
            add(Purchase(productList[0], 2, 15f))
            add(Purchase(productList[2], 1, 320f))
        }
        val purchaseList2 = mutableListOf<Purchase>().apply {
            add(Purchase(productList[0], 1, 21f))
            add(Purchase(productList[1], 1, 47f))
        }

        val orderList = mutableListOf<Order>().apply {
            add(Order(userDTOList[0], purchaseList1, OrderStatus.PAID, "Milan"))
            add(Order(userDTOList[1], purchaseList2, OrderStatus.DELIVERING, "Turin"))
        }
        orderRepo.saveAll(orderList)

        //test order POST
        val o = PlacedOrderDTO(userDTOList[1], purchaseList1.map { it.toDto() }, "Turin")
        val jsonString = Gson().toJson(o)
        println(jsonString)

        //Populate delivery
        //umbrella is shipped from asti, while tablet from Genova.
        //Only the first user has associated deliveries, the second user order is still in pending.
        val deliveryList = mutableListOf<Delivery>().apply{
            add(Delivery(orderList[0].id, "Milan", WarehouseDTO("Amazon", "Asti"), listOf(purchaseList1[0]), DeliveryStatus.PENDING))
            add(Delivery(orderList[0].id, "Milan", WarehouseDTO("Amazon", "Genova"), listOf(purchaseList1[1]), DeliveryStatus.PENDING))
            //add(Delivery(orderList[1].id, "Turin", WarehouseDTO("Ebay", "Roma"), listOf(purchaseList2[0]), DeliveryStatus.DELIVERING))
            //add(Delivery(orderList[1].id, "Turin", WarehouseDTO("Ebay", "Bari"), listOf(purchaseList2[1]), DeliveryStatus.PENDING))
        }

        val testDeliveryList = mutableListOf<Delivery>().apply{
            add(Delivery(o.toModel().id, "Turin", WarehouseDTO("Amazon", "Roma"), listOf(purchaseList1[0]), DeliveryStatus.PENDING))
            add(Delivery(o.toModel().id, "Turin", WarehouseDTO("Amazon", "Lecce"), listOf(purchaseList1[1]), DeliveryStatus.PENDING))
        }
        val d = DeliveryListDTO(o.toModel().id, testDeliveryList.map { it.toDto() })
        val jsonStringd = Gson().toJson(d)
        println(jsonStringd)
        deliveryRepo.saveAll(deliveryList)*/

    }
}


fun main(args: Array<String>) {
    runApplication<OrderserviceApplication>(*args)
}
