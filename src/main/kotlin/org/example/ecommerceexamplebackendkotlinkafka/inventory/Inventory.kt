package org.example.ecommerceexamplebackendkotlinkafka.inventory

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Inventory {
    @Id
    var skuId: String = ""  // Links to Product.skuId in MongoDB
    var stockLevel: Int = 0
}