package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    CANCELLED
}

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(nullable = false)
    val customerId: String,

    @Column(nullable = false)
    val productId: String,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    val totalAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)