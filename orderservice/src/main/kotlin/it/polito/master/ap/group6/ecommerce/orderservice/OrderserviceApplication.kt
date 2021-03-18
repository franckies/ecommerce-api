package it.polito.master.ap.group6.ecommerce.orderservice

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.ProductDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.orderservice.models.Order
import it.polito.master.ap.group6.ecommerce.orderservice.models.Purchase
import it.polito.master.ap.group6.ecommerce.orderservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.orderservice.repositories.OrderRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class OrderserviceApplication(
    orderRepo: OrderRepository
) {
    init {
        //clear table
        orderRepo.deleteAll()
        val userDTOList = mutableListOf<UserDTO>().apply {
            add(UserDTO("1239820421", "Francesco", "Semeraro", "Milano", "User"))
            add(UserDTO("2142109842", "Nicolò", "Chiapello", "Torino", "User"))
        }
        val productList = mutableListOf<ProductDTO>().apply {
            add(ProductDTO("Umbrella", "Repairs from rain", "Misc", "umbrella_pic", 20f))
            add(ProductDTO("Shoes", "Black shoes", "Dressing", "shoes_pic", 50f))
            add(ProductDTO("Tablet", "iPad 2018", "Eletronics", "tablet_pic", 300f))
        }
        val purchaseList1 = mutableListOf<Purchase>().apply {
            add(Purchase(productList[0], 2, 15f))
            add(Purchase(productList[2], 1, 320f))
        }
        val purchaseList2 = mutableListOf<Purchase>().apply {
            add(Purchase(productList[0], 1, 21f))
            add(Purchase(productList[1], 1, 47f))
        }

        //populate product table
        val orderList = mutableListOf<Order>().apply {
            add(Order(userDTOList[0], purchaseList1, OrderStatus.PAID, "Milan"))
            add(Order(userDTOList[1], purchaseList2, OrderStatus.CANCELLED, "Turin"))
        }
        orderRepo.saveAll(orderList)

        //test POST
        val o = PlacedOrderDTO(userDTOList[1], purchaseList1.map { it.toDto() }, "Turin")
        val jsonString = Gson().toJson(o)
        println(jsonString)

    }

}


fun main(args: Array<String>) {
    runApplication<OrderserviceApplication>(*args)
}
