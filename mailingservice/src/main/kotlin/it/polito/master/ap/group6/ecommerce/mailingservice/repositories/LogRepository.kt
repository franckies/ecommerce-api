package it.polito.master.ap.group6.ecommerce.mailingservice.repositories

import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import it.polito.master.ap.group6.ecommerce.mailingservice.model.MailingLog
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LogRepository: MongoRepository<MailingLog, ObjectId> {

    @Query("{ 'orderID' : ?0, 'type' : 'ALARMINFO' , 'productID': ?1, 'warehouse': ?2 }")
    fun getAlarmInfoMailingLogByOrderID(orderID: String, productID: String, warehouse: String) : Optional<MailingLog>

    @Query("{'orderID' : ?0 , 'type' : 'ORDERINFO', 'status' : ?1 }")
    fun getOrderInfoMailingLogByOrderIDAndStatus(orderID: String, orderStatus: OrderStatus) : Optional<MailingLog>
}