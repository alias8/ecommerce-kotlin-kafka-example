package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

@Entity
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var orderId: Long = 0
    var customerEmail: String = ""
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "order_id")
    var items: List<CartItem> = emptyList()
    var totalPrice: Double = 0.0
    var status: OrderStatus = OrderStatus.PENDING
}

enum class OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED
}

@Entity
class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var skuId: String = ""
    var unitPrice: Double = 0.0
    var quantity: Int = 0
}