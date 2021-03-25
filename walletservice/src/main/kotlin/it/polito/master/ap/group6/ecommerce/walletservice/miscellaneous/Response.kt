package it.polito.master.ap.group6.ecommerce.walletservice.miscellaneous

class Response(
    var body: Any? = null,
    val responseId: ResponseType? = null,
    val message: String? = null
) {
    companion object{
         fun userWalletCreated(): Response{
            return Response(body = null,ResponseType.USER_WALLET_CREATED, "The user wallet has been created.")
        }
        fun userWalletCreatedFailed(): Response{
            return Response(body = null,ResponseType.USER_WALLET_FAILED, "The user has already a wallet.")
        }
        fun userWalletGet(): Response{
            return Response(body = null,ResponseType.USER_WALLET_GET, "The userID wallet is.")
        }
        fun userWalletRecharge(): Response{
            return Response(body = null,ResponseType.USER_WALLET_RECHARGE, "The userID wallet is.")
        }
        fun userWalletPostTransaction(): Response{
            return Response(body = null,ResponseType.USER_WALLET_TRANSACTION, "The transaction has been added and pending for confirmation.")
        }
        fun userWalletNoMoney(): Response{
            return Response(body = null,ResponseType.USER_WALLET_NOMONEY, "There are not enough money to perform the transaction.")
        }
        fun userWalletConfirmTransaction(): Response{
            return Response(body = null,ResponseType.USER_WALLET_CONFIRM, "The requested transaction has been confirmed.")
        }
        fun userWalletFailed(): Response{
            return Response(body = null,ResponseType.USER_WALLET_FAILED, "There are no existing wallet for this userID.")
        }
        fun userWalletRefund(): Response{
            return Response(body = null,ResponseType.USER_WALLET_REFUND, "The rollback operation on orderID has been perfomed.")
        }

    }
}

enum class ResponseType{
    USER_WALLET_CREATED,
    USER_WALLET_FAILED,
    USER_WALLET_GET,
    USER_WALLET_RECHARGE,
    USER_WALLET_TRANSACTION,
    USER_WALLET_NOMONEY,
    USER_WALLET_CONFIRM,
    USER_WALLET_REFUND
}