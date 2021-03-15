package it.polito.master.ap.group6.ecommerce.common.dtos

data class UserDTO(
    val id: String? = null,
    val name: String? = null,
    val surname: String,
    val address: String?,
    val role: String?
)