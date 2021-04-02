package it.polito.master.ap.group6.ecommerce.common.dtos

data class UserDTO(
    val id: String? = null,
    val name: String,
    val surname: String,
    val username: String,
    val address: String?,
    val email: String? = null,
    val role: String?
)