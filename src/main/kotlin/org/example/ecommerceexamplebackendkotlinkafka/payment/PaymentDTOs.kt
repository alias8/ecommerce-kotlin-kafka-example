package org.example.ecommerceexamplebackendkotlinkafka.payment

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest

data class PaymentCreatedEvent(
    val success: Boolean,
    val cartItems: List<CartItemRequest>,
    val orderId: Long,
    val customerEmail: String,
)