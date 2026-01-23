package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

enum class OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED
}

@Entity
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var orderId: Long = 0
    var customerEmail: String = ""
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "order_id")
    var cartItems: MutableList<CartItem> = emptyList<CartItem>().toMutableList()
    var status: OrderStatus = OrderStatus.PENDING
}

@Entity
class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var quantity: Int = 0
    @ManyToOne
    var product: Product? = null
}

@Entity
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var skuId: String = ""
    var unitPrice: Double = 0.0
}


