//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.miscellaneous

//------- external dependencies ------------------------------------------------
import org.springframework.http.HttpStatus


//======================================================================================================================
//   Data structure
//======================================================================================================================
data class ExecutionResult<T> (
    val code: ExecutionResultType? = null,
    var body: T? = null,
    val message: String? = null,
    val http_code: HttpStatus? = null
)


//======================================================================================================================
//   Type definition
//======================================================================================================================
enum class ExecutionResultType{
    CORRECT_EXECUTION,
    GENERIC_ERROR,
    HTTP_ERROR,
    MISSING_IN_DB,
    EXTERNAL_HOST_NOT_REACHABLE,
    SOMEONE_ELSE_PROBLEM,
    PAYMENT_REFUSED,
    WITHDRAWAL_REFUSED
}