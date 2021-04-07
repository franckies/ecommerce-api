//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResult
import it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous.ExecutionResultType
import it.polito.master.ap.group6.ecommerce.catalogservice.models.Operation
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.OperationRepository
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import org.springframework.kafka.core.KafkaTemplate


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface OrderService {
    fun createOrderSync(userID: ObjectId, placedOrderDTO: PlacedOrderDTO): ExecutionResult<OrderDTO>
    fun createOrderAsync(userID: ObjectId, placedOrderDTO: PlacedOrderDTO): ExecutionResult<Any?>
    fun readOrderHistory(userID: ObjectId): ExecutionResult<ShownOrderListDTO>
    fun undoOrderSync(orderID: ObjectId): ExecutionResult<OrderDTO>
    fun undoOrderAsync(orderID: ObjectId): ExecutionResult<OrderDTO>
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
    @Autowired private val kafkaTemplateOrder: KafkaTemplate<String, String>,
    @Value("\${application.order_service}") private var orderservice_url: String = "localhost:8082"
) : OrderService {

    //------- attributes -------------------------------------------------------
    @Autowired
    lateinit var operationRepository: OperationRepository

    val mapper = jacksonObjectMapper()


    //------- methods ----------------------------------------------------------
    override fun createOrderSync(userID: ObjectId, placedOrderDTO: PlacedOrderDTO): ExecutionResult<OrderDTO> {
        // check if user exists
        val user = userService.get(userID)
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
        val creation_op = Operation(sagaId = sagaId, placedOrderDto = filled_dto)  // TODO insert SAGA identifier and formalize the SAGA object
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

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = order_dto)
    }

    override fun createOrderAsync(userID: ObjectId, placedOrderDTO: PlacedOrderDTO): ExecutionResult<Any?> {
        // check if user exists
        val user = userService.get(userID)
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
        val creation_op = Operation(sagaId = sagaId, placedOrderDto = filled_dto)
        operationRepository.save(creation_op)

        // emit the Saga Object on the Kafka topic
        val serialized_placedorder: String? = mapper.writeValueAsString(filled_dto)  //TODO: try Serializable on PlacedOrderDTO
        //kafkaTemplateOrder.send("create_order", filled_dto)
        val topic: String = "create_order"
        println("Emitting on topic '$topic', message $serialized_placedorder")
        kafkaTemplateOrder.send(topic, serialized_placedorder)

        // provide requested outcome
        return ExecutionResult(code = ExecutionResultType.CORRECT_EXECUTION, body = sagaId.toString())  // being async, nothing can go wrong and there is no immediate answer
    }

    override fun readOrderHistory(userID: ObjectId): ExecutionResult<ShownOrderListDTO> {
        // check if user exists
        val user = userService.get(userID)
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

    override fun undoOrderSync(orderID: ObjectId): ExecutionResult<OrderDTO> {
        return _rollback(orderID,"http://${orderservice_url}/order/delete/$orderID/sync")
    }

    override fun undoOrderAsync(orderID: ObjectId): ExecutionResult<OrderDTO> {
        return _rollback(orderID, "http://${orderservice_url}/order/delete/$orderID/async")
    }


    //------- internal facilities ----------------------------------------------
    private fun _checkDeliveryAddress(userID: ObjectId, deliveryAddress: String?): Boolean {
        // check input parameters
        if (deliveryAddress == null)
            return false

        // retrieve user data from the DB
        val user = userService.get(userID)
        if (user.isEmpty)
            return false

        // check if selected address is valid
        return user.get().deliveryAddress == deliveryAddress
    }

    private fun _rollback(orderID: ObjectId, url: String): ExecutionResult<OrderDTO> {
        // check if exists SAGA for this order
        val saga_obj = operationRepository.findBySagaId(orderID)  // assuming sagaId==orderId
        if (saga_obj.isEmpty)
            return ExecutionResult(code = ExecutionResultType.MISSING_IN_DB, message = "There is no SAGA for ID $orderID")

        // log SAGA operation
        operationRepository.deleteBySagaId(orderID)  // TODO understand exact meaning

        // submit remotely to the Order microservice
        val url: String = url
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

}
