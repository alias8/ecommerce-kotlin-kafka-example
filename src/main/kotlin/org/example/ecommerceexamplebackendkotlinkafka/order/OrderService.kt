package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {
    fun createOrder(order: OrderRequest): Order {
        val newOrder = Order()
        newOrder.customerEmail = order.customerEmail
        val cartItems = order.cartItems.map { item ->
            val newCartItem = CartItem()
            newCartItem.quantity = item.quantity

            val product =
                productRepository.findBySkuId(item.skuId) ?: throw ProductNotFoundException("Product not found: ${item.skuId}")
            newCartItem.product = product

            return@map newCartItem
        }

        newOrder.cartItems = cartItems
        return orderRepository.save(newOrder)
    }
}