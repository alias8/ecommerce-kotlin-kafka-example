package org.example.ecommerceexamplebackendkotlinkafka.product

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ProductUpdateRequest(
    @field:NotBlank(message = "skuId is required")
    var skuId: String = "",
    @field:Valid
    @field:Min(value = 0, message = "unitPrice must be greater than or equal to 0")
    var unitPrice: Double? = null,
    var category: String? = null,
    var attributes: Map<String, String>? = null,
    var reviews: List<Review>? = null,
    var images: List<String>? = null,
)

