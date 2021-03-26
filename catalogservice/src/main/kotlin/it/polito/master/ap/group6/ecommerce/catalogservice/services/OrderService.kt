//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResult
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType
import it.polito.master.ap.group6.ecommerce.catalogservice.models.Operation
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.OperationRepository
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import org.springframework.data.repository.findByIdOrNull


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface OrderService {
    fun createOrder(userID: String, placedOrderDTO: PlacedOrderDTO): ExecutionResult<OrderDTO>
    fun readOrderHistory(userID: String): ExecutionResult<ShownOrderListDTO>
    fun undoOrder(orderID: String): ExecutionResult<OrderDTO>
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

    //------- attributes -------------------------------------------------------
    @Autowired
    lateinit var operationRepository: OperationRepository


    //------- methods ----------------------------------------------------------
    override fun createOrder(userID: String, placedOrderDTO: PlacedOrderDTO): ExecutionResult<OrderDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "User $userID does not exist")

        // check data coherence
        if (!_checkDeliveryAddress(userID, placedOrderDTO.deliveryAddress))
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "User $userID has not '${placedOrderDTO.deliveryAddress}' as delivery address")

        // initialize SAGA object
        val sagaId: ObjectId = ObjectId.get() // assuming it creates unique IDs -> it will be orderID
        val filled_dto = PlacedOrderDTO(
            sagaID = sagaId.toString(),
            userID = user.get().id,
            purchaseList = placedOrderDTO.purchaseList,
            deliveryAddress = placedOrderDTO.deliveryAddress
        )

        // log SAGA operation
        val creation_op = Operation(sagaId = sagaId, orderDto = null)  // TODO insert SAGA identifier and formalize the SAGA object
        operationRepository.save(creation_op)

        // submit remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/orders"
        var order_dto: OrderDTO? = null
        try {
            print("Performing POST on '$url'... ")
            order_dto = RestTemplate().postForObject(
                url,  // url
                filled_dto,  // request
                OrderDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Order service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                HttpStatus.PAYMENT_REQUIRED -> {
                    System.err.println("Wallet service does not accept the economical transaction")
                    return ExecutionResult(code = ExecutionResultType.PAYMENT_REFUSED)
                }
                HttpStatus.CONFLICT -> {
                    System.err.println("Warehouse service does not accept the product withdrawal")
                    return ExecutionResult(code = ExecutionResultType.WITHDRAWAL_REFUSED)
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

        //

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = order_dto)
    }

    override fun readOrderHistory(userID: String): ExecutionResult<ShownOrderListDTO> {
        // check if user exists
        val user_id = ObjectId(userID)
        val user = userService.get(user_id)
        if (user.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "User $userID does not exist")

        // ask remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/${user.get().id}/orders"
        var res: ShownOrderListDTO? = null
        try {
            print("Performing GET on '$url'... ")
            res = RestTemplate().getForObject(
                url,  // url
                ShownOrderListDTO::class.java  // responseType
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Order service had internal errors")
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

    override fun undoOrder(orderID: String): ExecutionResult<OrderDTO> {
        // check if exists SAGA for this order
        val order_id: ObjectId = ObjectId(orderID)
        val saga_obj = operationRepository.findBySagaId(order_id)  // assuming sagaId==orderId
        if (saga_obj.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "There is no SAGA for ID $order_id")

        // log SAGA operation
        operationRepository.deleteBySagaId(order_id)  // TODO understand exact meaning

        // submit remotely to the Order microservice
        val url: String = "http://${orderservice_url}/order/delete/$orderID"
        var res: OrderDTO? = null
        try {
            print("Performing GET on '$url'... ")
            res = RestTemplate().getForObject(  // TODO: change into DELETE
                url,  // url
                OrderDTO::class.java
            )
            println("done")
        } catch (e: ResourceAccessException) {
            System.err.println("impossible to reach remote host")
            return ExecutionResult(code = ExecutionResultType.EXTERNAL_HOST_NOT_REACHABLE)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    System.err.println("Order service had internal errors")
                    return ExecutionResult(code = ExecutionResultType.SOMEONE_ELSE_PROBLEM)
                }
                HttpStatus.PAYMENT_REQUIRED -> {
                    System.err.println("Wallet service does not accept the economical transaction")
                    return ExecutionResult(code = ExecutionResultType.PAYMENT_REFUSED)
                }
                HttpStatus.CONFLICT -> {
                    System.err.println("Warehouse service does not accept the product withdrawal")
                    return ExecutionResult(code = ExecutionResultType.WITHDRAWAL_REFUSED)
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


    //------- internal facilities ----------------------------------------------

    private fun _checkDeliveryAddress(userID: String, deliveryAddress: String?): Boolean {
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
