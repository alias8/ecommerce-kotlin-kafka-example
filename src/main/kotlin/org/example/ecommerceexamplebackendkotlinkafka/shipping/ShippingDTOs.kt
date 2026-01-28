package org.example.ecommerceexamplebackendkotlinkafka.shipping

import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRelatedEvent

data class LowStockEvent(
    val skuId: String,
    val quantity: Int, override val orderId: Long?
) : OrderRelatedEvent

data class WarehouseEvent(
    val skuId: String,
    val quantity: Int,
    override val orderId: Long?,
) : OrderRelatedEvent

data class ShippingNotificationMessage(
    val skuId: String,
    val quantity: Int,
    val orderId: Long,
)