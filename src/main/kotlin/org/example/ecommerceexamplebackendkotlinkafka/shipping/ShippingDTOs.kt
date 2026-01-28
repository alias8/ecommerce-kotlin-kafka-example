package org.example.ecommerceexamplebackendkotlinkafka.shipping

data class LowStockEvent(
    val skuId: String,
    val quantity: Int
)

data class WarehouseEvent(
    val skuId: String,
    val quantity: Int,
    val orderId: Long,
)

data class ShippingNotificationMessage(
    val skuId: String,
    val quantity: Int,
    val orderId: Long,
)