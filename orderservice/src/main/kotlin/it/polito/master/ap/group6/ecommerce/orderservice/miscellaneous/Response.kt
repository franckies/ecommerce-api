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
        fun orderFound(): Response{
            return Response(body= null, ResponseType.ORDER_FOUND, "The order has been found.")
        }
        fun cannotRestoreProducts(): Response{
            return Response(body= null, ResponseType.CANNOT_RESTORE_PRODUCTS, "Cannot contact warehouse to restore products")
        }
        fun cannotRestoreMoney(): Response {
            return Response(body= null, ResponseType.CANNOT_RESTORE_MONEY, "Cannot contact wallet to restore money")
        }
        fun cannotReachTheMS(): Response {
            return Response(body= null, ResponseType.CANNOT_REACH_REMOTE_MS, "Cannot reach the remote ms")
        }
        fun moneyLocked(): Response {
            return Response(body= null, ResponseType.MONEY_LOCKED, "Money successfully locked on user wallet")
        }
        fun walletNotFound(): Response {
            return Response(body = null, ResponseType.WALLET_NOT_FOUND, "The wallet of the user doesn't exist")
        }
        fun orderSubmitted(): Response{
            return Response(body=null, ResponseType.ORDER_SUBMITTED, "The order has been submitted")
        }
        fun orderConfirmed(): Response{
            return Response(body=null, ResponseType.ORDER_CONFIRMED, "The order has been confirmed")
        }
        fun deliveriesUndone(): Response{
            return Response(body=null, ResponseType.DELIVERIES_UNDONE, "The deliveries associated to the order have been canceled successfully")
        }

        fun transactionUndone(): Response {
            return Response(body=null, ResponseType.TRANSACTION_UNDONE, "The transaction have been undone successfully")
        }

    }
}

enum class ResponseType{
    NO_MONEY,
    NO_PRODUCTS,
    ORDER_CREATED,
    ORDER_NOT_FOUND,
    USER_NOT_FOUND,
    ORDER_FOUND,
    CANNOT_RESTORE_MONEY,
    CANNOT_RESTORE_PRODUCTS,
    CANNOT_REACH_REMOTE_MS,
    MONEY_LOCKED,
    WALLET_NOT_FOUND,
    ORDER_SUBMITTED,
    DELIVERIES_UNDONE,
    ORDER_CONFIRMED,
    TRANSACTION_UNDONE
}