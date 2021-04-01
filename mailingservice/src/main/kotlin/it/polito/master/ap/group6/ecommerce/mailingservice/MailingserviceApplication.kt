package it.polito.master.ap.group6.ecommerce.mailingservice

import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MailingserviceApplication() {
    init {
    }
}

fun main(args: Array<String>) {
    runApplication<MailingserviceApplication>(*args)

}
