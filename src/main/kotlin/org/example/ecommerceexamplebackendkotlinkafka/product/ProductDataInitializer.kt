package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class ProductDataInitializer(private val productRepository: ProductRepository) : CommandLineRunner {
    override fun run(vararg args: String) {
        if (productRepository.count() == 0L) {
            val product1 = Product().apply {
                skuId = "SKU001"
                unitPrice = 30.0
                stockLevel = 100
            }
            val product2 = Product().apply {
                skuId = "SKU002"
                unitPrice = 50.0
                stockLevel = 100
            }
            productRepository.saveAll<Product>(listOf(product1, product2))
        }

    }
}