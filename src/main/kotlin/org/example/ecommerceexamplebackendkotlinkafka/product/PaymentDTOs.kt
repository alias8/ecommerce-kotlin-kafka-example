package org.example.ecommerceexamplebackendkotlinkafka.product

data class ProductUpdatedEvent(
    val orderId: Long,
    val customerEmail: String,
    val skuId: String,
    val quantity: Int,
    val sufficientStock: Boolean
)