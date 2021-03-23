//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.controllers

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WarehouseService
import it.polito.master.ap.group6.ecommerce.common.dtos.*


//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * Exposes Catalog endpoints, but aimed to the Warehouse microservice.
 * @param warehouseService a reference to the Service handling the business logic.
 *
 * @author Nicolò Chiapello
 */
@RestController
@RequestMapping("/catalog/products")  // root endpoint
class WarehouseController(
    @Autowired private val warehouseService: WarehouseService
) {

    /**
     * Shows the catalog (the same for all users).
     * @return the list of the available products (despite their location).
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @GetMapping("/show")
    fun showProducts(): ProductListDTO {

        // invoke the business logic
        val products_list = warehouseService.showProducts()

        // check the result
        if (products_list.isPresent)
            return products_list.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Shows the catalog for an admin user, with warehouse information.
     * @return the list of the available products for each warehouse.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @GetMapping("/admin/show")
    fun showProductsPerWarehouse(): ProductAdminListDTO {

        // invoke the business logic
        val products_list = warehouseService.showProductsPerWarehouse()

        // check the result
        if (products_list.isPresent)
            return products_list.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Admin adds a product specifying the warehouse.
     * @return the DTO corresponding to the created product.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @PostMapping("/admin")
    fun createProduct(@RequestBody newProduct: ProductAdminDTO): ProductDTO {

        // invoke the business logic
        val created_product = warehouseService.createProduct(newProduct)

        // check the result
        if (created_product.isPresent)
            return created_product.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }


    /**
     * Admin modify information of an existing product (eventually updating the alarm level).
     * @return the DTO corresponding to the updated product.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @PutMapping("/admin/{productID}")
    fun createProduct(@PathVariable("productID") productID: String,
                      @RequestBody modifiedProduct: ProductAdminDTO): ProductDTO {

        // invoke the business logic
        val updated_product = warehouseService.modifyProduct(productID, modifiedProduct)

        // check the result
        if (updated_product.isPresent)
            return updated_product.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}
