package org.example.ecommerceexamplebackendkotlinkafka.product

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest

data class ProductUpdatedEvent(
    val orderId: Long,
    val customerEmail: String,
    val skuId: String,
    val quantity: Int,
    val sufficientStock: Boolean
)