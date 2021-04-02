package it.polito.master.ap.group6.ecommerce.warehouseservice

import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.WarehouseStock
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.DeliveryLogRepository
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import springfox.documentation.swagger2.annotations.EnableSwagger2


@SpringBootApplication
@EnableMongoRepositories
@EnableSwagger2
class WarehouseserviceApplication(warehouseRepository: WarehouseRepository, deliveryLogRepository: DeliveryLogRepository) {

    init {
        populateCollections(warehouseRepository, deliveryLogRepository)
    }
}

fun populateCollections(warehouseRepository: WarehouseRepository, deliveryLogRepository: DeliveryLogRepository) {

    warehouseRepository.deleteAll()
    deliveryLogRepository.deleteAll()

    val productList = mutableListOf<Product>().apply {
        add(
            Product(
                name = "PlayStation",
                category = "Tech",
                currentPrice = 300.0f,
                stock = mutableListOf<WarehouseStock>(
                    WarehouseStock(warehouseName = "w1", warehouseAddress = "via Roma", availableQuantity = 100, alarmLevel = 20),
                    WarehouseStock(warehouseName = "w3", warehouseAddress = "via Milano", availableQuantity = 250, alarmLevel = 50)
                )
            )
        )
        add(
            Product(
                name = "Xbox",
                category = "Tech",
                currentPrice = 200.0f,
                stock = mutableListOf<WarehouseStock>(
                    WarehouseStock(warehouseName = "w1", warehouseAddress = "via Roma", availableQuantity = 50, alarmLevel = 10),
                    WarehouseStock(warehouseName = "w2", warehouseAddress = "via Torino", availableQuantity = 100, alarmLevel = 20)
                )
            )
        )
        add(
            Product(
                name = "T-Shirt",
                category = "Wearing",
                currentPrice = 50.0f,
                stock = mutableListOf<WarehouseStock>(
                    WarehouseStock(warehouseName = "w2", warehouseAddress = "via Torino", availableQuantity = 30, alarmLevel = 10),
                    WarehouseStock(warehouseName = "w3", warehouseAddress = "via Milano", availableQuantity = 20, alarmLevel = 10)
                )
            )
        )
    }

    warehouseRepository.saveAll(productList)

}

fun main(args: Array<String>) {
    runApplication<WarehouseserviceApplication>(*args)


}
