package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : MongoRepository<Product, String> {
    fun findBySkuId(skuId: String): Product?
}