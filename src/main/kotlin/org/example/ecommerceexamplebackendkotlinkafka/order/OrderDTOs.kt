package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.Embeddable
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class OrderRequest(
    @field:NotBlank(message = "customerEmail is required")
    @field:Email(message = "customerEmail must be a valid email address")
    val customerEmail: String?,

    @field:NotBlank(message = "paymentToken is required")
    val paymentToken: String?,

    @field:NotEmpty(message = "cartItems cannot be empty")
    @field:Valid
    val cartItems: List<CartItemRequest>? = null,

    // Set internally when checkout came through a stock reservation, so it can be released once payment succeeds.
    val reservationId: String? = null
)

@Embeddable
data class CartItemRequest(
    @field:NotBlank(message = "skuId is required")
    val skuId: String?,

    @field:Min(value = 1, message = "quantity must be at least 1")
    val quantity: Int = 0
)

data class OrderStatusUpdateRequest(
    @field:NotNull(message = "status is required")
    val status: OrderStatus?
)

data class OrderCreatedEvent(
    override val orderId: Long,
    val customerEmail: String,
    val paymentToken: String,
    val totalPrice: Double,
    val cartItems: List<CartItemRequest>,
    val reservationId: String? = null
) : OrderRelatedEvent

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

interface OrderRelatedEvent {
    val orderId: Long?
}