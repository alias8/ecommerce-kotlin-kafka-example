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
    var paymentToken: String = ""
}

@Entity
class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var quantity: Int = 0
    var unitPrice: Double = 0.0
    var skuId: String? = null      // Business identifier
    var productId: String? = null // MongoDB document ID
}

//@JvmInline
//value class ProductId(val value: String)
//
//@JvmInline
//value class SkuId(val value: String)


