package it.polito.master.ap.group6.ecommerce.common.misc

enum class OrderStatus {
    PAID,
    DELIVERING,
    DELIVERED,
    FAILED,
    CANCELED,
    PENDING
}
enum class DeliveryStatus {
    PENDING,
    DELIVERING,
    DELIVERED,
    CANCELED
}
enum class TransactionStatus {
    PENDING,
    ACCEPTED,
    REFUSED,
    REFUNDED
}
enum class UserRole {
    CUSTOMER,
    ADMIN,
    //TODO: SERVICE role needed to inter service communication?
}
