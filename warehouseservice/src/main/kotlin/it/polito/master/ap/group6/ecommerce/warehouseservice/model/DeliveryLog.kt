package it.polito.master.ap.group6.ecommerce.warehouseservice.model

import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryListDTO
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.sql.Timestamp
import java.util.*

@Document
data class DeliveryLog(

    @Id val id: ObjectId? = null,
    val orderID : String? = null,
    val deliveries : List<DeliveryDTO>? = null,
    val status : DeliveryLogStatus? =null,
    val timestamp: Date? = null

)

enum class DeliveryLogStatus {
    SHIPPED,
    CANCELED,
}