package it.polito.master.ap.group6.ecommerce.common.dtos

class ShownOrderDTO (
    val shownOrder: List<OrderDTO>? = null
)

data class ShownOrderListDTO(
    val shownOrderList : List<List<OrderDTO>>? = null
)
