package org.example.ecommerceexamplebackendkotlinkafka.payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    // Spring auto-generates all basic CRUD operations!
}