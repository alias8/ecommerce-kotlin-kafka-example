package org.example.ecommerceexamplebackendkotlinkafka.notification

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var orderId: Long? = null
    var skuId: String? = null
    var quantity: Int = 0
    var itemShippedEmailSent: Boolean = false
}