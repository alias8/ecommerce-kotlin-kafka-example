package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class OrderService(
    private val orderFromStoreRepository: OrderFromStoreRepository,
    private val productRepository: ProductRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OrderService::class.java)
    }

    fun createOrder(order: OrderRequest): OrderFromStore {
        val savedOrder =  orderFromStoreRepository.save(OrderFromStore().apply {
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
        logger.info("Order id ${savedOrder.orderId} saved")
        return savedOrder
    }
}