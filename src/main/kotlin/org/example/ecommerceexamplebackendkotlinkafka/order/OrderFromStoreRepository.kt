package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderFromStoreRepository : JpaRepository<OrderFromStore, Long> {
    // Spring auto-generates all basic CRUD operations!
}