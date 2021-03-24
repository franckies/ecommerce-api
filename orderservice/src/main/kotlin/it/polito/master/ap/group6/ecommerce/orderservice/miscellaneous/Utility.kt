package it.polito.master.ap.group6.ecommerce.orderservice.miscellaneous

import it.polito.master.ap.group6.ecommerce.common.misc.DeliveryStatus
import it.polito.master.ap.group6.ecommerce.common.misc.OrderStatus

class Utility {
    companion object{
        /**
         * Given an order status and a delivery status, getResultingStatus computes the status to be showed
         * to the client for a single delivery of an order. Notice that if getResultingStatus is invoked,
         * we're sure that there is at least one delivery associated to the order.
         * @param orderStatus, the status of the order
         * @param deliveryStatus, the status of the delivery of that order
         * @return the resulting status to be shown.
         */
        fun getResultingStatus(orderStatus: OrderStatus, deliveryStatus: DeliveryStatus): OrderStatus{
            if(deliveryStatus.equals(DeliveryStatus.DELIVERED))
                return OrderStatus.DELIVERED
            if(deliveryStatus.equals(DeliveryStatus.DELIVERING))
                return OrderStatus.DELIVERING
            if(deliveryStatus.equals(DeliveryStatus.PENDING))
                return OrderStatus.PAID
            if(deliveryStatus.equals(DeliveryStatus.CANCELED))
                return OrderStatus.CANCELED
            println("An error occurred retrieving the status.")
            return OrderStatus.FAILED
        }
    }
}