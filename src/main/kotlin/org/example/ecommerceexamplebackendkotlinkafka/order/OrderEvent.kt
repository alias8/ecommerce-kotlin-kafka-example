package org.example.ecommerceexamplebackendkotlinkafka.order

import java.math.BigDecimal
import java.time.Instant

data class OrderCreatedEvent(
    val orderId: String,
    val customerId: String,
    val productId: String,
    val quantity: Int,
    val totalAmount: BigDecimal,
    val createdAt: Instant
)