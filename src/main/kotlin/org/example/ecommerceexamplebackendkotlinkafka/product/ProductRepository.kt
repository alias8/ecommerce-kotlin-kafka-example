package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findBySkuId(skuId: String): Product?
}