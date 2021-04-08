package it.polito.master.ap.group6.ecommerce.mailingservice.controllers

import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.mailingservice.repositories.MailingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/mailing")
class MailingController(
    @Autowired private val mailingRepository: MailingRepository
) {
    /**
     * Create a new user in the mailing database.
     */
    @PostMapping("/create")
    fun createUsers(@RequestBody userDTO: UserDTO?): Unit {
        mailingRepository.save(userDTO!!)
    }
}