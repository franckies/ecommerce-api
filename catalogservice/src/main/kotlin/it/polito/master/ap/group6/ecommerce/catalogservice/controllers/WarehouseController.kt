//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.controllers

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest
import javax.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import mu.KotlinLogging
import org.bson.types.ObjectId
import java.lang.IllegalArgumentException

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType
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
    //------- attributes -------------------------------------------------------
    private val logger = KotlinLogging.logger {}


    //------- methods ----------------------------------------------------------
    /**
     * Shows the catalog (the same for all users).
     * @return the list of the available products (despite their location).
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @GetMapping("/show")
    fun showProducts(): ResponseEntity<ProductListDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received GET on url='${currentRequest?.requestURL}'" }

        // invoke the business logic
        val products_list = warehouseService.showProducts()

        // check the result
        return when (products_list.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(products_list.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.MISSING_IN_DB -> ResponseEntity(null, HttpStatus.NOT_FOUND)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, products_list.http_code!!)
            else -> ResponseEntity(products_list.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Shows the catalog for an admin user, with warehouse information.
     * @return the list of the available products for each warehouse.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @RolesAllowed("ROLE_ADMIN")
    @GetMapping("/admin/show")
    fun showProductsPerWarehouse(): ResponseEntity<ProductListWarehouseDTO> {
//    fun showProductsPerWarehouse(): ResponseEntity<ProductListAdminDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received GET on url='${currentRequest?.requestURL}'" }

        // invoke the business logic
        val products_list = warehouseService.showProductsPerWarehouse()

        // check the result
        return when (products_list.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(products_list.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.MISSING_IN_DB -> ResponseEntity(null, HttpStatus.NOT_FOUND)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, products_list.http_code!!)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(products_list.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Admin adds a product specifying the warehouse.
     * @return the DTO corresponding to the created product.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @RolesAllowed("ROLE_ADMIN")
    @PostMapping("/admin")
    fun createProduct(@RequestBody newProduct: ProductAdminDTO): ResponseEntity<ProductDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received POST on url='${currentRequest?.requestURL}' with body=${newProduct}" }

        // invoke the business logic
        val created_product = warehouseService.createProduct(newProduct)

        // check the result
        return when (created_product.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(created_product.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.MISSING_IN_DB -> ResponseEntity(null, HttpStatus.NOT_FOUND)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, created_product.http_code!!)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(created_product.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    /**
     * Admin modify information of an existing product (eventually updating the alarm level).
     * @return the DTO corresponding to the updated product.
     * @throws HttpStatus.NOT_FOUND if the remote microservice doesn't respond.
     */
    @RolesAllowed("ROLE_ADMIN")
    @PutMapping("/admin/{productID}")
    fun modifyProduct(@PathVariable("productID") productID: String,
                      @RequestBody modifiedProduct: ProductAdminDTO): ResponseEntity<ProductDTO> {
        // log incoming request
        val currentRequest: HttpServletRequest? = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        logger.info { "Received PUT on url='${currentRequest?.requestURL}' with body=${modifiedProduct}" }

        // cast input parameters
        val product_id: ObjectId = try {
            ObjectId(productID)
        } catch (e: IllegalArgumentException) {
            logger.error { "Impossible to convert $productID into ObjectID" }
            return ResponseEntity(null, HttpStatus.BAD_REQUEST)
        }

        // invoke the business logic
        val updated_product = warehouseService.modifyProduct(product_id, modifiedProduct)

        // check the result
        return when (updated_product.code) {
            ExecutionResultType.CORRECT_EXECUTION -> ResponseEntity(updated_product.body, HttpStatus.OK)
            ExecutionResultType.GENERIC_ERROR -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.MISSING_IN_DB -> ResponseEntity(null, HttpStatus.NOT_FOUND)
            ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.SOMEONE_ELSE_PROBLEM -> ResponseEntity(null, HttpStatus.INTERNAL_SERVER_ERROR)
            ExecutionResultType.HTTP_ERROR -> ResponseEntity(null, updated_product.http_code!!)
            ExecutionResultType.PAYMENT_REFUSED -> ResponseEntity(null, HttpStatus.PAYMENT_REQUIRED)
            ExecutionResultType.WITHDRAWAL_REFUSED -> ResponseEntity(null, HttpStatus.CONFLICT)
            else -> ResponseEntity(updated_product.body, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}
