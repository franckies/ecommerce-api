package it.polito.master.ap.group6.ecommerce.mailingservice.listeners

import com.google.gson.Gson
import it.polito.master.ap.group6.ecommerce.common.dtos.MailingInfoDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.PlacedOrderDTO
import it.polito.master.ap.group6.ecommerce.mailingservice.model.MailType
import it.polito.master.ap.group6.ecommerce.mailingservice.model.MailingLog
import it.polito.master.ap.group6.ecommerce.mailingservice.repositories.LogRepository
import it.polito.master.ap.group6.ecommerce.mailingservice.repositories.MailingRepository
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.*
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * The mail listener exposes the method that sends an email each time a variation in the order
 * status is registered (i.e. published on a kafka topic).
 * @param mailingRepository the repository storing the users.
 * @author Francesco Semeraro
 */
@Service
class MailListener(
    @Autowired private val mailingRepository: MailingRepository,
    @Autowired private val logRepository : LogRepository
) {
    private val json = Gson()
    @KafkaListener(groupId = "mailingservice", topics = ["order_tracking"])
    fun sendInfoMail(mailingInfoDTOSer: String) {
        val mailingInfoDTO: MailingInfoDTO = json.fromJson(mailingInfoDTOSer, MailingInfoDTO::class.java)

        val mailingLog : Optional<MailingLog>
        try {
            mailingLog = logRepository.getOrderInfoMailingLogByOrderIDAndStatus(mailingInfoDTO.orderId!!, orderStatus = mailingInfoDTO.orderStatus!!)
            if (!mailingLog.isEmpty) {
                println("ERROR: Order info mail already sent for this orderID.")
                return
            }
        } catch (e: Exception) {
            println("sendInfoMail Exception: $e")
            return
        }


        val optionalUser = mailingRepository.findById(ObjectId(mailingInfoDTO.userId))
        if (optionalUser.isEmpty) {
            println("MailListener.sendMail: there aren't user with id ${mailingInfoDTO.userId}")
            return
        }

        val user = optionalUser.get()
        val emailAddr: InternetAddress = InternetAddress(user.email)
        //check email validity
        try {
            emailAddr.validate()
        } catch (e: AddressException) {
            println("MailListener.sendMail: the email ${user.email} is invalid")
            return
        }
        //send email
        try {
            val email: Email = SimpleEmail()
            email.setHostName("smtp.googlemail.com")
            email.setSmtpPort(465)
            email.setAuthenticator(DefaultAuthenticator("noreply.ecommerceapi@gmail.com", "noreply.ecommerceapi1222"))
            email.isSSLOnConnect = true
            email.setFrom("noreply_ecommerceapi@gmail.com")
            email.subject = "News about your order ${mailingInfoDTO.orderId}"
            val textMessage: String =
                """
                    Hi ${user.name}! 
                    ${mailingInfoDTO.message}
                    Your order number ${mailingInfoDTO.orderId} is now in the status ${mailingInfoDTO.orderStatus}.
                    For any question, do not hesitate to contact us at our call-center +39 3473102002.
            """
            email.setMsg(textMessage)
            email.addTo(emailAddr.toString())
            email.send()
            println("sendInfoMail : mail sent.")
            val mailingLog = MailingLog(orderID = mailingInfoDTO.orderId, type = MailType.ORDERINFO, status = mailingInfoDTO.orderStatus)
            logRepository.save(mailingLog)
            println("sendInfoMail : log saved.")
        } catch (e: Exception) {
            println("MailListener.sendMail: {${e.cause}. Impossible to send the email.")
        }
    }

    @KafkaListener(groupId = "mailingservice", topics = ["alarm_level"])
    fun sendAlarmMail(mailingInfoString: String) {

        val mailingInfoDTO: MailingInfoDTO = json.fromJson(mailingInfoString, MailingInfoDTO::class.java)

        val mailingLog : Optional<MailingLog>
        try {
            mailingLog = logRepository.getAlarmInfoMailingLogByOrderID(mailingInfoDTO.orderId!!, mailingInfoDTO.productID!!, mailingInfoDTO.warehouse!!)
            if (!mailingLog.isEmpty) {
                println("ERROR: Alarm Level mail already sent for this orderID.")
                return
            }
        } catch (e: Exception) {
            println("sendAlarmMail Exception: $e")
            return
        }

        val alarmInfo = mailingInfoDTO.message

        val optionalAdmins = mailingRepository.findUserDTOByRole("ADMIN")
        if (optionalAdmins.isEmpty) {
            println("MailListener.sendMail: there aren't admins in the database.")
            return
        }
        val admins = optionalAdmins.get()
        //define the email
        try {
            val email: Email = SimpleEmail()
            email.setHostName("smtp.googlemail.com")
            email.setSmtpPort(465)
            email.setAuthenticator(DefaultAuthenticator("noreply.ecommerceapi@gmail.com", "noreply.ecommerceapi1222"))
            email.isSSLOnConnect = true
            email.setFrom("noreply_ecommerceapi@gmail.com")
            email.subject = "Alarm level information"
            email.addTo("noreply_ecommerceapi@gmail.com")
            val textMessage: String =
                """
                    Hi! 
                    $alarmInfo
                """
            email.setMsg(textMessage)
            //check address validity and add to bcc
            val emailAddrList: List<InternetAddress> = admins.map { InternetAddress(it.email) }

            for (emailAddr: InternetAddress in emailAddrList) {
                try {
                    emailAddr.validate()
                    email.addBcc(emailAddr.toString())

                } catch (e: AddressException) {
                    println("MailListener.sendMail: the email $emailAddr is invalid")
                    continue
                }
            }
            email.send()
            println("sendAlarmMail : mail sent.")
            val mailingLog = MailingLog(orderID = mailingInfoDTO.orderId, type = MailType.ALARMINFO, productID = mailingInfoDTO.productID, warehouse = mailingInfoDTO.warehouse)
            logRepository.save(mailingLog)
            println("sendAlarmMail : log saved.")
        } catch (e: Exception) {
            println("MailListener.sendMail: {${e.cause}. Impossible to send the email.")
        }
    }
}
