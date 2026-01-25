package org.example.ecommerceexamplebackendkotlinkafka.product

data class ProductUpdatedEvent(
    val orderId: Long,
    val customerEmail: String,
    val stockLevel: Int,
    val skuId: String,
)