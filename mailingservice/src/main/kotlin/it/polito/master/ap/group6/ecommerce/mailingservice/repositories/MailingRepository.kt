package it.polito.master.ap.group6.ecommerce.mailingservice.repositories

import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface MailingRepository: MongoRepository<UserDTO, ObjectId>{}