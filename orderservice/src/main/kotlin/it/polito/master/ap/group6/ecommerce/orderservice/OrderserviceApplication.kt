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
            add(UserDTO("saddas", "Francesco", "Semeraro", "Milano", "User"))
            add(UserDTO("sar32", "Nicolò", "Chiapello", "Torino", "User"))
        }
        val productList = mutableListOf<ProductDTO>().apply {
            add(ProductDTO("prod1", "desc1", "a", "asc", 20f))
            add(ProductDTO("prod2", "desc2", "b", "asc23", 50f))
            add(ProductDTO("prod3", "desc3", "c", "asc23", 10f))
        }
        val purchaseList = mutableListOf<Purchase>().apply {
            add(Purchase(productList[0], 5, 16f))
            add(Purchase(productList[2], 1, 47f))
        }

        //populate product table
        val orderList = mutableListOf<Order>().apply {
            add(Order(userDTOList[0], purchaseList, OrderStatus.PAID, "Milan"))
            add(Order(userDTOList[1], purchaseList, OrderStatus.CANCELLED, "Turin"))
        }
        orderRepo.saveAll(orderList)

        //test POST
        val o = PlacedOrderDTO(userDTOList[0], purchaseList.map { it.toDto() }, "Milan")
        val jsonString = Gson().toJson(o)
        println(jsonString)

    }

}


fun main(args: Array<String>) {
    runApplication<OrderserviceApplication>(*args)
}
