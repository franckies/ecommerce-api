package it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous

import it.polito.master.ap.group6.ecommerce.common.dtos.OrderDTO

class Response(
    var body: Any? = null,
    val responseId: ResponseType? = null,
    val message: String? = null
) {
    companion object{
        fun notEnoughMoney(): Response {
            return Response(body = null,ResponseType.NO_MONEY, "The user hasn't enough money to complete the order")
        }
        fun productNotAvailable(): Response{
            return Response(body = null,ResponseType.NO_PRODUCTS, "One or more products in the order are no more available.")
        }
        fun orderCreated(): Response{
            return Response(body = null,ResponseType.ORDER_CREATED, "The order has been successfully created!")
        }
        fun orderCannotBeFound(): Response{
            return Response(body = null,ResponseType.ORDER_NOT_FOUND, "The order cannot be found.")
        }
        fun userCannotBeFound(): Response{
            return Response(body = null,ResponseType.USER_NOT_FOUND, "The user cannot be found.")
        }

    }
}

enum class ResponseType{
    NO_MONEY,
    NO_PRODUCTS,
    ORDER_CREATED,
    ORDER_NOT_FOUND,
    USER_NOT_FOUND
}