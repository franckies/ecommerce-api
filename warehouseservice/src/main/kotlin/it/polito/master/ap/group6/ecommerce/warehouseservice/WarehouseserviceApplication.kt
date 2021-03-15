package it.polito.master.ap.group6.ecommerce.warehouseservice

import it.polito.master.ap.group6.ecommerce.common.dtos.WarehouseDTO
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WarehouseserviceApplication

fun main(args: Array<String>) {
    runApplication<WarehouseserviceApplication>(*args)
    var w = WarehouseDTO()
    println(w)

}
