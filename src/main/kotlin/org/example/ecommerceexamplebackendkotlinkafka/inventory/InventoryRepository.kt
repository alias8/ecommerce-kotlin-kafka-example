package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface InventoryRepository : JpaRepository<Inventory, String> {
    fun findBySkuId(skuId: String): Inventory?

    // Atomic decrement - only updates if sufficient stock
    @Modifying
    @Query("UPDATE Inventory i SET i.stockLevel = i.stockLevel - :quantity WHERE i.skuId = :skuId AND i.stockLevel >= :quantity")
    fun decrementStock(skuId: String, quantity: Int): Int
}