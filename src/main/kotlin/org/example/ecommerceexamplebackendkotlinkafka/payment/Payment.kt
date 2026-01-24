package org.example.ecommerceexamplebackendkotlinkafka.payment

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var orderId: Long = 0
    var totalPrice: Double = 0.0
    var paymentToken: String = ""
    var success: Boolean = false
}