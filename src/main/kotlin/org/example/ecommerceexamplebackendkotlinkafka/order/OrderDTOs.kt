package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.Embeddable
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class OrderRequest(
    @field:NotBlank(message = "customerEmail is required")
    @field:Email(message = "customerEmail must be a valid email address")
    val customerEmail: String?,

    @field:NotBlank(message = "paymentToken is required")
    val paymentToken: String?,

    @field:NotEmpty(message = "cartItems cannot be empty")
    @field:Valid
    val cartItems: List<CartItemRequest>? = null
)

@Embeddable
data class CartItemRequest(
    @field:NotBlank(message = "skuId is required")
    val skuId: String?,

    @field:Min(value = 1, message = "quantity must be at least 1")
    val quantity: Int = 0
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
    const val PRODUCTS = "products"
    const val INVENTORY = "inventory"
    const val INVENTORY_ALERTS = "inventory-alerts"
    const val WAREHOUSE_ALERTS = "warehouse-alerts"
}

object KafkaGroupId {
    const val PAYMENT_SERVICE = "payment-service"
    const val PRODUCT_SERVICE = "product-service"
    const val SHIPPING_SERVICE = "shipping-service"
    const val INVENTORY_SERVICE = "inventory-service"
    const val NOTIFICATION_SERVICE = "notification-service"
}