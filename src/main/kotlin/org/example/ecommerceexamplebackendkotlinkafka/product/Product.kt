package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.Instant

@Document(collection = "products")
class Product : Serializable {
    @Id
    var id: String? = null
    var skuId: String = ""
    var unitPrice: Double = 0.0

    // Flexible attributes - different per product type
    var category: String = ""
    var attributes: Map<String, Any> = emptyMap()
    var reviews: List<Review> = emptyList()
    var images: List<String> = emptyList()

    companion object {
        private const val serialVersionUID = 1L
    }
}

data class Review(
    val userId: String,
    val rating: Int,
    val comment: String,
    val date: Instant
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}