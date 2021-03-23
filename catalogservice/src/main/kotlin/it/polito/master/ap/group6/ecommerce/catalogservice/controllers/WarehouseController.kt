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
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest
import javax.annotation.security.RolesAllowed

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.services.WarehouseService
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole



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
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received GET on url='${currentRequest?.requestURL}'")

        // invoke the business logic
        val products_list = warehouseService.showProducts()

        // check the result
        if (products_list.isPresent)
            return products_list.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "message")
    }


    /**
     * Shows the catalog for an admin user, with warehouse information.
     * @return the list of the available products for each warehouse.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @GetMapping("/admin/show")
    fun showProductsPerWarehouse(): ProductAdminListDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received GET on url='${currentRequest?.requestURL}'")

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
    @RolesAllowed("ADMIN")
    @PostMapping("/admin")
    fun createProduct(@RequestBody newProduct: ProductAdminDTO): ProductDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received POST on url='${currentRequest?.requestURL}' with body=${newProduct}")

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
    @RolesAllowed("ADMIN")
    @PutMapping("/admin/{productID}")
    fun createProduct(@PathVariable("productID") productID: String,
                      @RequestBody modifiedProduct: ProductAdminDTO): ProductDTO {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        println("Received PUT on url='${currentRequest?.requestURL}' with body=${modifiedProduct}")

        // invoke the business logic
        val updated_product = warehouseService.modifyProduct(productID, modifiedProduct)

        // check the result
        if (updated_product.isPresent)
            return updated_product.get()
        else
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}
