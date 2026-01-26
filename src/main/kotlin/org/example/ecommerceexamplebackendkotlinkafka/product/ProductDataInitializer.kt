package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProductDataInitializer(private val productRepository: ProductRepository) : CommandLineRunner {
    override fun run(vararg args: String) {
        if (productRepository.count() == 0L) {
            val product1 = Product().apply {
                skuId = "SHIRT001"
                unitPrice = 29.99
                stockLevel = 100
                category = "clothing"
                attributes = mapOf(
                    "size" to "L",
                    "color" to "blue",
                    "material" to "cotton"
                )
                images = listOf(
                    "https://example.com/shirt-front.jpg",
                    "https://example.com/shirt-back.jpg"
                )
                reviews = listOf(
                    Review("user123", 5, "Great quality!", Instant.now()),
                    Review("user456", 4, "Fits well", Instant.now())
                )
            }
            val product2 = Product().apply {
                skuId = "LAPTOP001"
                unitPrice = 1299.99
                stockLevel = 25
                category = "electronics"
                attributes = mapOf(
                    "brand" to "TechCorp",
                    "cpu" to "M3 Pro",
                    "ram" to "16GB",
                    "storage" to "512GB SSD"
                )
                images = listOf("https://example.com/laptop.jpg")
                reviews = emptyList()
            }
            productRepository.saveAll<Product>(listOf(product1, product2))
        }

    }
}