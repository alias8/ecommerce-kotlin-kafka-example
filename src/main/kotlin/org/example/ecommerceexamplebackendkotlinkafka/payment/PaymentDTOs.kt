package org.example.ecommerceexamplebackendkotlinkafka.payment

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRelatedEvent

data class PaymentCreatedEvent(
    val success: Boolean,
    val cartItems: List<CartItemRequest>,
    override val orderId: Long,
    val customerEmail: String,
) : OrderRelatedEvent