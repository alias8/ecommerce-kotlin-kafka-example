package org.example.ecommerceexamplebackendkotlinkafka.shipping

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderStatus

@Entity
class Shipping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var orderId: Long? = null
    var skuId: String? = null
    var quantity: Int = 0
    var status: OrderStatus = OrderStatus.PENDING
}