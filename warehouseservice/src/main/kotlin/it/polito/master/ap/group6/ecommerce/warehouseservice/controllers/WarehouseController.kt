package it.polito.master.ap.group6.ecommerce.warehouseservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.services.WarehouseService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class WarehouseController(val warehouseService: WarehouseService) {

    @GetMapping("/warehouse/products/totals")
    fun getProductsTotals(): ProductListDTO? {
        val productsTotal: ProductListDTO? = warehouseService.getProductsTotals()
        return productsTotal
    }

    @GetMapping("/warehouse/products/perwarehouse")
    fun getProductsPerWarehouse() : ProductListAdminDTO? {
        return warehouseService.getProductsPerWarehouse()
    }

    @PostMapping("/warehouse/products")
    fun insertNewProductInWarehouse(@RequestBody productAdminDTO : ProductAdminDTO) : ResponseEntity<ProductAdminDTO>? {
        val result = warehouseService.insertNewProductInWarehouse(productAdminDTO)
        return if (result!=null) {
            ResponseEntity.ok(result)
        } else {
            null
        }
    }

    @PostMapping("/warehouse/orders")
    fun getDeliveries(@RequestBody orderDTO: OrderDTO) : DeliveryListDTO? {
        return warehouseService.getDeliveries(orderDTO)
    }

    @PostMapping("/warehouse/orders/restore")
    fun cancelDeliveries(@RequestBody deliveryListDTO: DeliveryListDTO) : Boolean? {
        return warehouseService.updateStocksAfterDeliveriesCancellation(deliveryListDTO)
    }

    @PutMapping("/warehouse/products")
    fun updateProductInWarehouse(@RequestBody productAdminDTO: ProductAdminDTO, productID: String) : ResponseEntity<ProductAdminDTO>? {
        val result = warehouseService.updateProductInWarehouse(productID, productAdminDTO)
        return if (result!=null) {
            ResponseEntity.ok(result)
        } else {
            null
        }
    }

}