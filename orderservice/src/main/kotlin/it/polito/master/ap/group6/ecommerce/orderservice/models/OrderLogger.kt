package it.polito.master.ap.group6.ecommerce.orderservice.models

import it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous.OrderLoggerStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("orderLogger")
data class OrderLogger (
    @Id val orderID: String? = null,
    val orderStatus: OrderLoggerStatus? = null,
    val timestamp: Date? = null
)