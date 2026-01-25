package org.example.ecommerceexamplebackendkotlinkafka.product

class ProductNotFoundException(
    message: String,
) : RuntimeException(message)