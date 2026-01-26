package org.example.ecommerceexamplebackendkotlinkafka.product

import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : MongoRepository<Product, String> {
    fun findBySkuId(skuId: String): Product?
    // Atomic decrement - only updates if sufficient stock
    @Query("{ 'skuId': ?0, 'stockLevel': { '\$gte': ?1 } }")
    @Update("{ '\$inc': { 'stockLevel': ?2 } }")
    fun decrementStock(skuId: String, requiredStock: Int, decrementBy: Int): Long
    @Aggregation(pipeline = [
        "{ '\$unwind': '\$reviews' }",
        "{ '\$match': { 'reviews.userId': ?0 } }",
        "{ '\$replaceRoot': { 'newRoot': '\$reviews' } }"
    ])
    fun findReviewsByUserId(userId: String): List<Review>
    @Aggregation(pipeline = [
        "{ '\$unwind': '\$reviews' }",
        "{ '\$sort': { 'reviews.date': -1 } }",
        "{ '\$limit': 10 }",
        "{ '\$replaceRoot': { 'newRoot': '\$reviews' } }"
    ])
    fun findRecentReviews(): List<Review>
}