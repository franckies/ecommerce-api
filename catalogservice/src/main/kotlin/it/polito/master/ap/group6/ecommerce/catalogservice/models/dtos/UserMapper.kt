//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.models.dtos

//------- external dependencies ------------------------------------------------
import it.polito.master.ap.group6.ecommerce.catalogservice.models.User
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.misc.UserRole


//======================================================================================================================
//   Extension functions
//======================================================================================================================

fun User.toDto() : UserDTO {
    return UserDTO(
        id = this.id,
        name = this.name!!,
        surname = this.surname!!,
        username = this.username!!,
        address = this.deliveryAddress,
        role = this.role.toString()
    )
}

fun UserDTO.toModel(): User {
    return User(
        name = this.name,
        surname = this.surname,
        username = this.username,
        password = null,
        deliveryAddress = this.address,
        role = enumValueOf<UserRole>(this.role!!)
    )
}