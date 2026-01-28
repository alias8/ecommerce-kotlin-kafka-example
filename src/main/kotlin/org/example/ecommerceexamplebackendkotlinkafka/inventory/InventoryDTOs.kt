package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRelatedEvent

data class InventoryUpdatedEvent(
    override val orderId: Long,
    val customerEmail: String,
    val skuId: String,
    val quantity: Int,
    val sufficientStock: Boolean
) : OrderRelatedEvent