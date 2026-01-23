package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.persistence.*

enum class OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED
}

@Entity
@Table(name = "orders")
class OrderFromStore {
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


