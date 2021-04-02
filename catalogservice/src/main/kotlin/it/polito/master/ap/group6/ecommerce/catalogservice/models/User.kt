//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.models

//------- external dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document



//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * The user model. Describes an user with all the information needed to login.
 * @property name First name of the user.
 * @property surname Last name of the user.
 * @property username Unique nickname inside the platform.
 * @property password Login credentials.
 * @property deliveryAddress (optional) Place to deliver the submitted orders.
 * @property role Distinguish between standard buyers and system administrators.
 *
 * @author Nicolò Chiapello
 */
@Document("users")
class User {

    //------- attributes -------------------------------------------------------
    @Id
    var id: String? = null

    var name: String? = null
    var surname: String? = null
    var email: String? = null

    var username: String? = null
    var password: String? = null

    var deliveryAddress: String? = null
    var role: UserRole? = null

    //------- constructors -----------------------------------------------------
    constructor(name: String, surname: String, username: String?, password: String?,
                deliveryAddress: String? = null, email: String?, role: UserRole = UserRole.CUSTOMER) {
        this.name = name
        this.surname = surname
        this.username = username
        this.email = email
        this.password = password
        this.deliveryAddress = deliveryAddress
        this.role = role
    }

    //------- methods ----------------------------------------------------------
}