package org.example.ecommerceexamplebackendkotlinkafka.cart

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AddCartItemRequest(
    @field:NotBlank(message = "skuId is required")
    val skuId: String?,

    @field:Min(value = 1, message = "quantity must be at least 1")
    val quantity: Int = 0
)

data class UpdateCartItemRequest(
    @field:Min(value = 1, message = "quantity must be at least 1")
    val quantity: Int = 0
)

data class CheckoutRequest(
    @field:NotBlank(message = "paymentToken is required")
    val paymentToken: String?
)

data class CartItemView(
    val skuId: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

data class CartResponse(
    val items: List<CartItemView>,
    val total: Double
)
