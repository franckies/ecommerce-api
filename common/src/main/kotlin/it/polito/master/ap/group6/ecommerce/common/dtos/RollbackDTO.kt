package it.polito.master.ap.group6.ecommerce.common.dtos

import it.polito.master.ap.group6.ecommerce.common.misc.MicroService

data class RollbackDTO (
    val sagaID: String? = null,
    val sender: MicroService? = null,
)

