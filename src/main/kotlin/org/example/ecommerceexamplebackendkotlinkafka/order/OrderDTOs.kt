package org.example.ecommerceexamplebackendkotlinkafka.order

data class OrderRequest(
    val customerEmail: String,
    var cartItems: List<CartItemRequest> = emptyList()
)

data class CartItemRequest(
    val skuId: String,
    val quantity: Int
)