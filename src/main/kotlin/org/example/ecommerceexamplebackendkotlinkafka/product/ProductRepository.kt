package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : MongoRepository<Product, String> {
    @Cacheable("products")
    fun findBySkuId(skuId: String): Product?
}