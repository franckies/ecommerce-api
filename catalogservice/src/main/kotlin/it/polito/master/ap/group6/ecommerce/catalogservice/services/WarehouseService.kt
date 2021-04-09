//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.http.HttpStatus

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResult
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import org.bson.types.ObjectId


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface WarehouseService {
    fun showProducts(): ExecutionResult<ProductListDTO>
//    fun showProductsPerWarehouse(): ExecutionResult<ProductListAdminDTO>
    fun showProductsPerWarehouse(): ExecutionResult<ProductListWarehouseDTO>
    fun createProduct(product: ProductAdminDTO): ExecutionResult<ProductDTO>
    fun modifyProduct(productID: ObjectId, modified_product: ProductAdminDTO): ExecutionResult<ProductDTO>
}



//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic dealing with the external Warehouse microservice.
 * @property userService a reference to the Service handling the User CRUD operations.
 * @property warehouseservice_url the URL of the external Warehouse microservice, configurable by property file.
 *
 * @author Nicolò Chiapello
 */
@Service
class WarehouseServiceImpl(
    @Autowired private val userService: UserService,
    @Value("\${application.warehouse_service}") private var warehouseservice_url: String = "localhost:8084"
) : WarehouseService {

    //------- methods ----------------------------------------------------------

    override fun showProducts(): ExecutionResult<ProductListDTO> {
        // ask remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/totals"
        var res: ProductListDTO? = null
        try {
            print("Performing GET on '$url'... ")
            res = RestTemplate().getForObject(
                url,  // url
                ProductListDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Warehouse service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = res)
    }


//    override fun showProductsPerWarehouse(): ExecutionResult<ProductListAdminDTO> {
    override fun showProductsPerWarehouse(): ExecutionResult<ProductListWarehouseDTO> {

        // ask remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/perwarehouse"
//        var res: ProductListAdminDTO? = null
        var res : ProductListWarehouseDTO? = null
        try {
            print("Performing GET on '$url'... ")
            res = RestTemplate().getForObject(
                url,  // url
//                ProductListAdminDTO::class.java  // responseType
                ProductListWarehouseDTO::class.java
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Warehouse service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = res)
    }

    override fun createProduct(new_product: ProductAdminDTO): ExecutionResult<ProductDTO> {
        // submit remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products"
        var created_product: ProductDTO? = null
        try {
            print("Performing POST on '$url'... ")
            created_product = RestTemplate().postForObject(
                url,  // url
                new_product,  // request
                ProductDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Warehouse service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = created_product)
    }

    override fun modifyProduct(productID: ObjectId, modified_product: ProductAdminDTO): ExecutionResult<ProductDTO> {
        // submit remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/update/${productID}"
        var updated_product: ProductDTO? = null
        try {
            print("Performing POST on '$url'... ")
            updated_product = RestTemplate().postForObject(
                url,  // url
                modified_product,  // request
                ProductDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Warehouse service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                else -> {
                    System.err.println("obtained ${e.statusCode}")
                    return ExecutionResult(code = ExecutionResultType.HTTP_ERROR, http_code = e.statusCode)
                }
            }
        } catch (e: Exception) {
            System.err.println("encountered exception $e")
            return ExecutionResult(code = ExecutionResultType.GENERIC_ERROR, message = "Catch exception ${e.message}")
        }

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = updated_product)
    }

}
