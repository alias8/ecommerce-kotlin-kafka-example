package org.example.ecommerceexamplebackendkotlinkafka.order

class OrderNotFoundException(
    message: String,
) : RuntimeException(message)
