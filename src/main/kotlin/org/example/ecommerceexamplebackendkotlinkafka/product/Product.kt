package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "products")
class Product {
    @Id
    var id: String? = null
    var skuId: String = ""
    var unitPrice: Double = 0.0
    var stockLevel: Int = 0
}