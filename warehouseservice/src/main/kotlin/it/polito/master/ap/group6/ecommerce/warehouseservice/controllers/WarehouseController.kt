package it.polito.master.ap.group6.ecommerce.warehouseservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.WarehouseserviceApplication
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.services.WarehouseService
//import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Warehouse
//import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
//import it.polito.master.ap.group6.ecommerce.warehouseservice.services.WarehouseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class WarehouseController(val warehouseService: WarehouseService) {

    @GetMapping("/warehouse/products/totals")
    fun getProductsTotals(): ProductListDTO? {
        return warehouseService.getProductsTotals()
    }

    @GetMapping("/warehouse/products/perwarehouse")
    fun getProductsPerWarehouse() : ProductListAdminDTO? {
        return warehouseService.getProductsPerWarehouse()
    }

    @PostMapping("/warehouse/products")
    fun insertNewProduct(@RequestBody productAdminDTO : ProductAdminDTO) : ResponseEntity<ProductAdminDTO> {
        return warehouseService.insertNewProduct(productAdminDTO)
    }

    @PostMapping("/warehouse/orders")
    fun getDeliveries(@RequestBody orderDTO: OrderDTO) : DeliveryListDTO {
        return warehouseService.getDeliveries(orderDTO)
    }

//    @GetMapping("/debug")
//    fun getProduct(product : ProductDTO): Product? {
//        return warehouseService.getProduct(product)
//    }
//
//    @GetMapping("/debug2")
//    fun checkAvailability(orderDTO : OrderDTO) : Boolean {
//        return warehouseService.checkAvailability(orderDTO)
//    }


}