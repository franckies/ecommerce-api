//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.ResourceAccessException
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.common.dtos.*


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface WarehouseService {
    fun showProducts(): Optional<ProductListDTO>
    fun showProductsPerWarehouse(): Optional<ProductListAdminDTO>
    fun createProduct(product: ProductAdminDTO): Optional<ProductDTO>
    fun modifyProduct(productID: String, modified_product: ProductAdminDTO): Optional<ProductDTO>
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

    override fun showProducts(): Optional<ProductListDTO> {
        // ask remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/totals"
        var res: ProductListDTO? = null
        try {
            res = RestTemplate().getForObject(
                url,  // url
                ProductListDTO::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to GET from '$url'")
            return Optional.empty()
        }

        // provide requested outcome
        if (res == null)
            return Optional.empty()
        else
            return Optional.of(res)
    }

    override fun showProductsPerWarehouse(): Optional<ProductListAdminDTO> {
        // ask remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/perwarehouse"
        var res: ProductListAdminDTO? = null
        try {
            res = RestTemplate().getForObject(
                url,  // url
                ProductListAdminDTO::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to GET from '$url'")
            return Optional.empty()
        }

        // provide requested outcome
        if (res == null)
            return Optional.empty()
        else
            return Optional.of(res)
    }

    override fun createProduct(new_product: ProductAdminDTO): Optional<ProductDTO> {
        // submit remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products"
        var created_product: ProductDTO? = null
        try {
            created_product = RestTemplate().postForObject(
                url,  // url
                new_product,  // request
                ProductDTO::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to POST on '$url' the object:\n$new_product")
            return Optional.empty()
        }

        // provide requested outcome
        if (created_product == null)
            return Optional.empty()
        else
            return Optional.of(created_product)
    }

    override fun modifyProduct(productID: String, modified_product: ProductAdminDTO): Optional<ProductDTO> {
        // submit remotely to the Warehouse microservice
        val url: String = "http://${warehouseservice_url}/warehouse/products/${productID}"
        var updated_product: Unit? = null
        try {
            updated_product = RestTemplate().put(
                url,  // url
                modified_product,  // request
                ProductDTO::class.java  // responseType  //TODO check meaning of this parameter
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to PUT on '$url' the object:\n$updated_product")
            return Optional.empty()
        }

        // provide requested outcome
        if (updated_product == null)
            return Optional.empty()
        else
            return Optional.empty()  //TODO return Optional.of(updated_product)
    }

}
