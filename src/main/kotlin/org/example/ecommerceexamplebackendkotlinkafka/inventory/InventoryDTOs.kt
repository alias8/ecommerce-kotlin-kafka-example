package org.example.ecommerceexamplebackendkotlinkafka.inventory

data class InventoryUpdatedEvent(
    val orderId: Long,
    val customerEmail: String,
    val skuId: String,
    val quantity: Int,
    val sufficientStock: Boolean
)