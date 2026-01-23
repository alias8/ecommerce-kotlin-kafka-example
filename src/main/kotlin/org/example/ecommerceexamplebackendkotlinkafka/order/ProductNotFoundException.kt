package org.example.ecommerceexamplebackendkotlinkafka.order

class ProductNotFoundException(
    message: String,
) : RuntimeException(message)