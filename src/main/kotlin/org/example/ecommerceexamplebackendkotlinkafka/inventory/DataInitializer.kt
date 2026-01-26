package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.example.ecommerceexamplebackendkotlinkafka.product.Product
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductRepository
import org.example.ecommerceexamplebackendkotlinkafka.product.Review
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DataInitializer(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) : CommandLineRunner {
    override fun run(vararg args: String) {
        val seedData = listOf(
            SeedProduct(
                skuId = "SHIRT001",
                unitPrice = 29.99,
                stockLevel = 100,
                category = "clothing",
                attributes = mapOf("size" to "L", "color" to "blue", "material" to "cotton"),
                images = listOf("https://example.com/shirt-front.jpg", "https://example.com/shirt-back.jpg"),
                reviews = listOf(
                    Review("user123", 5, "Great quality!", Instant.now()),
                    Review("user456", 4, "Fits well", Instant.now())
                )
            ),
            SeedProduct(
                skuId = "LAPTOP001",
                unitPrice = 1299.99,
                stockLevel = 25,
                category = "electronics",
                attributes = mapOf("brand" to "TechCorp", "cpu" to "M3 Pro", "ram" to "16GB", "storage" to "512GB SSD"),
                images = listOf("https://example.com/laptop.jpg"),
                reviews = emptyList()
            )
        )

        // Initialize products (MongoDB)
        if (productRepository.count() == 0L) {
            val products = seedData.map { seed ->
                Product().apply {
                    skuId = seed.skuId
                    unitPrice = seed.unitPrice
                    category = seed.category
                    attributes = seed.attributes
                    images = seed.images
                    reviews = seed.reviews
                }
            }
            productRepository.saveAll(products)
        }

        // Initialize inventory (SQL)
        if (inventoryRepository.count() == 0L) {
            val inventory = seedData.map { seed ->
                Inventory().apply {
                    skuId = seed.skuId
                    stockLevel = seed.stockLevel
                }
            }
            inventoryRepository.saveAll(inventory)
        }
    }

    private data class SeedProduct(
        val skuId: String,
        val unitPrice: Double,
        val stockLevel: Int,
        val category: String,
        val attributes: Map<String, Any>,
        val images: List<String>,
        val reviews: List<Review>
    )
}