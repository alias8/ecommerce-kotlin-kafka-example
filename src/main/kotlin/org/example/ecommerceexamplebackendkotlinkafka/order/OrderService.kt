package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {
    fun createOrder(order: OrderRequest): Order = orderRepository.save(Order().apply {
        customerEmail = order.customerEmail
        cartItems = order.cartItems.map { item ->
            CartItem().apply {
                quantity = item.quantity
                product =
                    productRepository.findBySkuId(item.skuId)
                        ?: throw ProductNotFoundException("Product not found: ${item.skuId}")

            }
        }.toMutableList()
    })
}