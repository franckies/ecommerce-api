package it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous

class Response(
    val errorId: String? = null,
    val message: String? = null
) {
    companion object{
        fun notEnoughMoney(): Response {
            return Response("1000", "The user hasn't enough money to complete the order")
        }
        fun productNotAvailable(): Response{
            return Response("1001", "One or more products in the order are no more available.")
        }
        fun orderCreated(): Response{
            return Response("1010", "The order has been successfully created!")
        }
        fun orderCannotBeFound(): Response{
            return Response("1011", "The order cannot be found.")
        }
        fun userCannotBeFound(): Response{
            return Response("1100", "The user cannot be found.")
        }

    }
}