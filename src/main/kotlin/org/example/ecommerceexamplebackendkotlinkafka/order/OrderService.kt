package org.example.ecommerceexamplebackendkotlinkafka.order

import org.example.ecommerceexamplebackendkotlinkafka.product.ProductNotFoundException
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderFromStoreRepository: OrderFromStoreRepository,
    private val productRepository: ProductRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any> // Known error, can ignore
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OrderService::class.java)
    }

    init {
        logger.info("OrderService bean created!")
    }

    fun createOrder(order: OrderRequest): OrderFromStore {
        val savedOrder = saveToDatabase(order)
        sendKafkaEvent(savedOrder)
        return savedOrder
    }

    fun saveToDatabase(order: OrderRequest): OrderFromStore {
        val savedOrder = orderFromStoreRepository.save(OrderFromStore().apply {
            customerEmail = order.customerEmail!!
            paymentToken = order.paymentToken!!
            cartItems = order.cartItems!!.map { item ->
                CartItem().apply {
                    quantity = item.quantity
                    product =
                        productRepository.findBySkuId(item.skuId!!)
                            ?: throw ProductNotFoundException("Product not found: ${item.skuId}")

                }
            }.toMutableList()
        })
        return savedOrder
    }

    fun sendKafkaEvent(order: OrderFromStore) {
        val orderCreatedEvent = OrderCreatedEvent(
            orderId = order.orderId,
            paymentToken = order.paymentToken,
            customerEmail = order.customerEmail,
            totalPrice = order.cartItems.sumOf { it.product!!.unitPrice * it.quantity },
            cartItems = order.cartItems.map { CartItemRequest(it.product!!.skuId, it.quantity) }
        )
        kafkaTemplate.send(KafkaTopic.ORDERS, order.orderId.toString(), orderCreatedEvent)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Success Kafka event sent. Service: Order. Order id ${order.orderId}")
                } else {
                    logger.info("Failure Kafka event sent. Service: Order. Order id ${order.orderId}")
                }
            }

    }
}