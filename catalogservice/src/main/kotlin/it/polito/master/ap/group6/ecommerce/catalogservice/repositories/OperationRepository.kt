//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.repositories

//------- external dependencies ------------------------------------------------
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.Operation
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User


//======================================================================================================================
//   Interface
//======================================================================================================================
@Repository("operations")
interface OperationRepository: MongoRepository<Operation, ObjectId> {
    // custom queries
    fun findBySagaId(sagaId: ObjectId): Optional<Operation>
    fun deleteBySagaId(sagaId: ObjectId)
}
