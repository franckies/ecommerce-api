package it.polito.master.ap.group6.ecommerce.warehouseservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.services.WarehouseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class WarehouseController(val warehouseService: WarehouseService) {

    @GetMapping("/warehouse/products/totals")
    fun getProductsTotals(): ResponseEntity<ProductListDTO> {
        println("WarehouseService.getProductsTotals() invoked.")
        val productsTotal: ProductListDTO = warehouseService.getProductsTotals()
        return ResponseEntity(productsTotal, HttpStatus.OK)
    }

    @GetMapping("/warehouse/products/perwarehouse")
    fun getProductsPerWarehouse() : ResponseEntity<ProductListAdminDTO> {
        println("WarehouseService.getProductsPerWarehouse() invoked.")
        val productsPerWarehouse = warehouseService.getProductsPerWarehouse()
        return ResponseEntity(productsPerWarehouse, HttpStatus.OK)
    }

    @PostMapping("/warehouse/products")
    fun insertNewProductInWarehouse(@RequestBody productAdminDTO : ProductAdminDTO) : ResponseEntity<ProductAdminDTO>? {
        println("WarehouseService.insertNewProductInWarehouse() invoked.")
        val result = warehouseService.insertNewProductInWarehouse(productAdminDTO)
        return if (result!=null) {
            println("WarehouseService.insertNewProductInWarehouse() : returning OK.")
            ResponseEntity(result, HttpStatus.OK)
        } else {
            println("WarehouseService.insertNewProductInWarehouse() : returning CONFLICT.")
            ResponseEntity(result, HttpStatus.CONFLICT)
        }
    }

    @PostMapping("/warehouse/orders")
    fun getDeliveries(@RequestBody orderDTO: OrderDTO) : ResponseEntity<DeliveryListDTO?> {
        println("WarehouseService.getDeliveries() invoked.")
        val result = warehouseService.getDeliveries(orderDTO)
        return if (result!=null) {
            println("WarehouseService.getDeliveries() : returning OK.")
            ResponseEntity(result, HttpStatus.OK)
        } else {
            println("WarehouseService.getDeliveries() : returning CONFLICT.")
            ResponseEntity(result, HttpStatus.CONFLICT)
        }
    }

    @PostMapping("/warehouse/orders/restore")
    fun cancelDeliveries(@RequestBody deliveryListDTO: DeliveryListDTO) : Boolean? {
        return warehouseService.updateStocksAfterDeliveriesCancellation(deliveryListDTO)
    }

    @PutMapping("/warehouse/products")
    fun updateProductInWarehouse(@RequestBody productAdminDTO: ProductAdminDTO, productID: String) : ResponseEntity<ProductAdminDTO>? {
        println("WarehouseService.updateProductInWarehouse() invoked.")
        val result = warehouseService.updateProductInWarehouse(productID, productAdminDTO)
        return if (result!=null) {
            println("WarehouseService.updateProductInWarehouse() : returning OK ")
            ResponseEntity(result, HttpStatus.OK)
        } else {
            println("WarehouseService.updateProductInWarehouse() : returning NOT FOUND.")
            ResponseEntity(result, HttpStatus.NOT_FOUND)
        }
    }

}