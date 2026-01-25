package org.example.ecommerceexamplebackendkotlinkafka.shipping

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShippingRepository : JpaRepository<Shipping, Long>