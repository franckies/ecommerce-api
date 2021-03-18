package it.polito.master.ap.group6.ecommerce.orderservice.models

import it.polito.master.ap.group6.ecommerce.common.dtos.ProductDTO
import it.polito.master.ap.group6.ecommerce.common.dtos.UserDTO
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * The order model. Describes an order as it is saved in the database.
 * @param buyer The user owner of the order
 * @param purchases The purchases in the order. A purchase is composed by a product, a quantity, and the selling price
 * @param status The status of the order. One among PAID, DELIVERING, DELIVERED, CANCELED.
 * @param deliveryAddress The delivery address where to ship the order.
 * @property price The sum of the selling prices per each quantity of each product in the purchases.
 * @author Francesco Semeraro
 *
 */
@Document("orders")
class Order(
    val buyer: UserDTO? = null,
    val purchases: List<Purchase>? = null,
    var status: OrderStatus? = null,
    val deliveryAddress: String? = null
) {
    @Id
    var id: String? = null
    val price: Float?
        get() = purchases?.map { it.price * it.quantity.toFloat() }?.sum()
}

data class Purchase(val product: ProductDTO, val quantity: Int, val price: Float)