package it.polito.master.ap.group6.ecommerce.common.misc

enum class OrderStatus {
    PAID,
    DELIVERING,
    DELIVERED,
    FAILED,
    CANCELLED
}

enum class UserRole {
    CUSTOMER,
    ADMIN,
    //TODO: SERVICE role needed to inter service communication?
}
