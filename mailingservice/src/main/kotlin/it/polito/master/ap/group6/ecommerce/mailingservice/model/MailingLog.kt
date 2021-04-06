package it.polito.master.ap.group6.ecommerce.mailingservice.model

import it.polito.master.ap.group6.ecommerce.common.dtos.DeliveryDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/* Keep track of sent mails */

@Document
data class MailingLog (

    @Id
    val id: ObjectId? = null,
    val orderID : String? = null,
    val type : MailType? = null,
    val status: OrderStatus? = null,
    val timestamp: Date = Date()

)

enum class MailType {
    ALARMINFO,
    ORDERINFO,
}