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



//======================================================================================================================
//   Interface
//======================================================================================================================
@Repository("users")
interface OperationRepository: MongoRepository<Operation, ObjectId> {
    // custom queries
    fun deleteBySagaId(sagaId: ObjectId)
}
