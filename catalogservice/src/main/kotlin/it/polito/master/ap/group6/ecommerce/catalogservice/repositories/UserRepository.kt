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
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User



//======================================================================================================================
//   Interface
//======================================================================================================================
@Repository("users")
interface UserRepository: MongoRepository<User, ObjectId> {
    // custom queries
    fun findByUsername(username: String): Optional<User>
    fun deleteByUsername(username: String)
}
