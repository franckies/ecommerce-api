package it.polito.master.ap.group6.ecommerce.mailingservice

import it.polito.master.ap.group6.ecommerce.mailingservice.repositories.MailingRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MailingserviceApplication(
    private val mailingRepository: MailingRepository
) {
    init {
        // clear database
        mailingRepository.deleteAll()
    }
}

fun main(args: Array<String>) {
    runApplication<MailingserviceApplication>(*args)

}
