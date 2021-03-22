//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.services

//------- external dependencies ------------------------------------------------
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

//------- internal dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.catalogservice.repositories.UserRepository
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole


//======================================================================================================================
//   Abstract declaration
//======================================================================================================================
interface UserService {
    fun create(name: String, surname: String, username: String, password: String,
               deliveryAddress: String? = null, role: UserRole = UserRole.CUSTOMER): User

    fun get(userID: ObjectId): Optional<User>
    fun get(username: String): Optional<User>

    fun exists(userID: ObjectId): Boolean
    fun exists(username: String): Boolean

    fun delete(userID: ObjectId)
    fun delete(username: String)

    fun clear()
}



//======================================================================================================================
//   Concrete implementation
//======================================================================================================================
/**
 * The business logic for the CRUD operations on the User collection.
 * @param userRepository a reference to the Repository handling the database interaction.
 * @param walletService a reference to the Service handling the communication toward the WalletService microservice.
 *
 * @author Nicolò Chiapello
 */
@Service
class UserServiceImpl(
    @Autowired private val userRepository: UserRepository,
    //@Autowired private val walletService: WalletService
) : UserService {

    override fun create(
        name: String,
        surname: String,
        username: String,
        password: String,
        deliveryAddress: String?,
        role: UserRole
    ): User {
        // create live object
        val user = User(name, surname, username, password, deliveryAddress, role)

        // store in database
        val user_with_id = userRepository.save(user)

        // trigger wallet creation
        //walletService.createWallet(user_with_id)  // FIXME: referencing walletService create a loop in the dependencies

        // provide requested outcome
        return user_with_id
    }

    override fun get(userID: ObjectId): Optional<User> =
        userRepository.findById(userID)
    override fun get(username: String): Optional<User> =
        userRepository.findByUsername(username)

    override fun exists(userID: ObjectId): Boolean {
        val response = get(userID)
        return response.isPresent
    }
    override fun exists(username: String): Boolean {
        val response = get(username)
        return response.isPresent
    }

    override fun delete(userID: ObjectId) =
        userRepository.deleteById(userID)
    override fun delete(username: String) =
        userRepository.deleteByUsername(username)

    override fun clear() =
        userRepository.deleteAll()

}