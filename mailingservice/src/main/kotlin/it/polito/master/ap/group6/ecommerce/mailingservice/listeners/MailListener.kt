package it.polito.master.ap.group6.ecommerce.mailingservice.listeners

import it.polito.master.ap.group6.ecommerce.common.dtos.MailingInfoDTO
import it.polito.master.ap.group6.ecommerce.mailingservice.repositories.MailingRepository
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
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
    @Autowired private val mailingRepository: MailingRepository
) {
    @KafkaListener(groupId = "ecommerce", topics = ["order_tracking"])
    fun sendMail(mailingInfoDTO: MailingInfoDTO) {
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
            email.setAuthenticator(DefaultAuthenticator("noreply_ecommerceapi@gmail.com", "noreply.ecommerceapi1222"))
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
        } catch (e: Exception) {
            println("MailListener.sendMail: {${e.cause}. Impossible to send the email.")
        }
    }
}
