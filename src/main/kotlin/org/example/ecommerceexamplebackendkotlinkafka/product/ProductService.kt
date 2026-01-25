package org.example.ecommerceexamplebackendkotlinkafka.product

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderCreatedEvent
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderService
import org.example.ecommerceexamplebackendkotlinkafka.order.ProductNotFoundException
import org.example.ecommerceexamplebackendkotlinkafka.payment.PaymentCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import kotlin.String

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProductService::class.java)
    }

    init {
        logger.info("ProductService bean created!")
    }

    @KafkaListener(topics = [KafkaTopic.PAYMENTS], groupId = KafkaGroupId.PRODUCT_SERVICE)
    fun handlePaymentCreated(event: PaymentCreatedEvent) {
        if(!event.success) return

        for(item in event.cartItems) {
            val productToUpdate = productRepository.findBySkuId(item.skuId!!)
            ?: throw ProductNotFoundException("Product not found: ${item.skuId}")
            productToUpdate.stockLevel -= item.quantity
            productRepository.save(productToUpdate)

            val productUpdatedEvent = ProductUpdatedEvent(
                orderId = event.orderId,
                customerEmail = event.customerEmail,
                stockLevel = item.quantity,
                skuId = item.skuId
            )
            kafkaTemplate.send(KafkaTopic.PRODUCTS, event.orderId.toString(), productUpdatedEvent)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        logger.info("Success Kafka event sent. Service: Product. Order id ${event.orderId}")
                    } else {
                        logger.info("Failure Kafka event sent. Service: Product. Order id ${event.orderId}")
                    }
                }
        }
    }
}