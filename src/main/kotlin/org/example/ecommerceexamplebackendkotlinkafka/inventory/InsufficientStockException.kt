package org.example.ecommerceexamplebackendkotlinkafka.inventory

class InsufficientStockException(val skuIds: List<String>) :
    RuntimeException("Insufficient stock for: ${skuIds.joinToString()}")
