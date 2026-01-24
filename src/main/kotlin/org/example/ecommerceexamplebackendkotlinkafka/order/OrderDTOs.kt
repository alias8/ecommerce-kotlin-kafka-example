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

data class OrderCreatedEvent (
    val orderId: Long,
    val customerEmail: String,
    val paymentToken: String ,
    val totalPrice: Double ,
)

object KafkaTopic {
    const val ORDERS = "orders"
}