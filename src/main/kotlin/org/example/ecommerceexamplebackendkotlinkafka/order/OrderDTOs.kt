package org.example.ecommerceexamplebackendkotlinkafka.order

data class OrderRequest(
    val customerEmail: String,
    val paymentToken: String,
    var cartItems: List<CartItemRequest> = emptyList()
)

data class CartItemRequest(
    val skuId: String,
    val quantity: Int
)

data class OrderCreatedEvent(
    val orderId: Long,
    val customerEmail: String,
    val paymentToken: String,
    val totalPrice: Double,
    val cartItems: List<CartItemRequest>
)

object KafkaTopic {
    const val ORDERS = "orders"
    const val PAYMENTS = "payments"
}

object KafkaGroupId {
    const val PAYMENT_SERVICE = "payment-service"
}