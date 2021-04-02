//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.models

//------- external dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*


//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * The logging model. Describes a performed operation and its related SAGA identifier.
 * @property id Mongo primary key.
 * @property sagaId identifier of the transaction.
 * @property orderDto reference to the performed operation.
 * @property timestamp timestamp at which operation has been performed.
 *
 * @author Nicolò Chiapello
 */
@Document("operations")
class Operation {

    //------- attributes -------------------------------------------------------
    @Id
    var id: String? = null  // Mongo primary key

    var sagaId: ObjectId? = null  // identifier of the transaction
    var placedOrderDto: PlacedOrderDTO? = null  // reference to the performed operation

    var timestamp: Date = Date()

    //------- constructors -----------------------------------------------------
    constructor(sagaId: ObjectId?, placedOrderDto: PlacedOrderDTO?) {
        this.sagaId = sagaId
        this.placedOrderDto = placedOrderDto
    }
}