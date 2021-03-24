//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.ResourceAccessException
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos.toDto
import it.polito.master.ap.group6.ecommerce.common.dtos.*


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface OrderService {
    fun createOrder(userID: String, placedOrderDTO: PlacedOrderDTO): Optional<OrderDTO>
    fun readOrderHistory(userID: String): Optional<PlacedOrderListDTO>
    fun undoOrder(orderID: String): Optional<OrderDTO>
}



//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic dealing with the external Order microservice.
 * @property userService a reference to the Service handling the User CRUD operations.
 * @property orderservice_url the URL of the external Order microservice, configurable by property file.
 *
 * @author Nicolò Chiapello
 */
@Service
class OrderServiceImpl(
    @Autowired private val userService: UserService,
    @Value("\${application.order_service}") private var orderservice_url: String = "localhost:8082"
) : OrderService {

    //------- methods ----------------------------------------------------------

    override fun createOrder(userID: String, placedOrderDTO: PlacedOrderDTO): Optional<OrderDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return Optional.empty()

        // fill information of the user
        if ( !checkDeliveryAddress(userID, placedOrderDTO.deliveryAddress) )
            return Optional.empty()
        val filled_dto = PlacedOrderDTO(
            user = user.get().toDto(),
            purchaseList = placedOrderDTO.purchaseList,
            deliveryAddress = placedOrderDTO.deliveryAddress  //TODO let the client decide or force by server-side?
        )

        // submit remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/orders"
        var order_dto: OrderDTO? = null
        try {
            order_dto = RestTemplate().postForObject(
                url,  // url
                filled_dto,  // request
                OrderDTO::class.java  // responseType
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to POST on '$url' the object:\n$filled_dto")
            return Optional.empty()
        }

        // provide requested outcome
        if (order_dto != null)
            return Optional.of(order_dto)
        else
            return Optional.empty()
    }

    override fun readOrderHistory(userID: String): Optional<PlacedOrderListDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return Optional.empty()

        // ask remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/orders/${user.get().id}"
        var res: PlacedOrderListDTO? = null
        try {
            res = RestTemplate().getForObject(
                url,  // url
                PlacedOrderListDTO::class.java  // responseType
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

    override fun undoOrder(orderID: String): Optional<OrderDTO> {
        // submit remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/orders/$orderID"
        var res: Unit? = null  //TODO how to force returning an OrderDTO object
        try {
            res = RestTemplate().delete(
                url  // url
            )
        } catch (e: ResourceAccessException) {
            System.err.println("Impossible to DELETE from '$url'")
            return Optional.empty()
        }

        // provide requested outcome
        if (res == null)
            return Optional.empty()
        else
            return Optional.empty() //TODO return Optional.of(res)
    }


    //------- internal facilities ----------------------------------------------

    fun checkDeliveryAddress(userID: String, deliveryAddress: String?): Boolean {
        // check input parameters
        if (deliveryAddress == null)
            return false

        // retrieve user data from the DB
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return false

        // check if selected address is valid
        return user.get().deliveryAddress == deliveryAddress
    }

}
