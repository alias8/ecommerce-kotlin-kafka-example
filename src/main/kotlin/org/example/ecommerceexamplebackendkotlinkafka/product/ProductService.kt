package org.example.ecommerceexamplebackendkotlinkafka.product

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProductService::class.java)
    }

    init {
        logger.info("ProductService bean created!")
    }

    fun getProduct(skuId: String): Product {
        return productRepository.findBySkuId(skuId)
            ?: throw ProductNotFoundException("Product not found: $skuId")
    }

    @CacheEvict("products", key = "#productUpdateRequest.skuId")
    fun updateProduct(productUpdateRequest: ProductUpdateRequest): Product {
        val product = productRepository.findBySkuId(productUpdateRequest.skuId)
            ?: throw ProductNotFoundException("Product not found: ${productUpdateRequest.skuId}")
        productUpdateRequest.unitPrice?.let { product.unitPrice = it }
        productUpdateRequest.category?.let { product.category = it }
        productUpdateRequest.attributes?.let { product.attributes = it }
        productUpdateRequest.reviews?.let { product.reviews = it }
        productUpdateRequest.images?.let { product.images = it }
        return productRepository.save(product)

    }
}