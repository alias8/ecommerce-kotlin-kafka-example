package org.example.ecommerceexamplebackendkotlinkafka.product

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var skuId: String = ""
    var unitPrice: Double = 0.0
    var stockLevel: Int = 0
}