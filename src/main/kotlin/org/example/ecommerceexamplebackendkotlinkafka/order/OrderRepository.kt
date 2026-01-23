package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, String> {
    fun findByCustomerId(customerId: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
}