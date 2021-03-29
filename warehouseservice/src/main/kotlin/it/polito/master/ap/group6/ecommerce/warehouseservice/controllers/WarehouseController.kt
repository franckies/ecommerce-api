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
        val response : ResponseEntity<DeliveryListDTO?>
        val result = warehouseService.getDeliveries(orderDTO.orderID!!, orderDTO.purchases)
        if (result==null) {
            println("WarehouseService.getDeliveries() : returning CONFLICT.")
            response = ResponseEntity(null, HttpStatus.CONFLICT)
        } else {
            if (result.deliveryList!=null) {
                println("WarehouseService.getDeliveries() : returning OK.")
                response = ResponseEntity(result, HttpStatus.OK)
            } else {
                println("WarehouseService.getDeliveries() : returning CONFLICT.")
                response = ResponseEntity(null, HttpStatus.CONFLICT)
            }
        }
        return response
    }

    @GetMapping("/warehouse/orders/restore/{orderID}")
    fun cancelDeliveries(@PathVariable("orderID") orderID: String) : ResponseEntity<Boolean> {
        println("WarehouseService.cancelDeliveries() invoked.")
        val res = warehouseService.updateStocksAfterDeliveriesCancellation(orderID)
        if (res==true) {
            println("WarehouseService.cancelDeliveries() : returning OK ")
            return ResponseEntity(null, HttpStatus.OK)
        } else {
            println("WarehouseService.cancelDeliveries() : returning NOT_ACCEPTABLE ")
            return ResponseEntity(null, HttpStatus.NOT_ACCEPTABLE)
        }
    }

    @PostMapping("/warehouse/products/update")
    fun updateProductInWarehouse(@RequestBody productAdminDTO: ProductAdminDTO, productID: String?) : ResponseEntity<ProductAdminDTO>? {
        println("WarehouseService.updateProductInWarehouse() invoked.")
        if (productID==null)
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
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