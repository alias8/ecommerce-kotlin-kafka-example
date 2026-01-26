package org.example.ecommerceexamplebackendkotlinkafka.product

import jakarta.transaction.Transactional
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.payment.PaymentCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

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

    fun examples() {
        productRepository.findReviewsByUserId("user123") // example to show complex query
    }

    fun decrementStock(skuId: String, requiredStock: Int): Long {
        return productRepository.decrementStock(skuId, requiredStock, -requiredStock)
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopic.PAYMENTS],
        groupId = KafkaGroupId.PRODUCT_SERVICE,
        containerFactory = "filterSuccessMessagesKafkaListenerContainerFactory"
    )
    fun handlePaymentCreated(event: PaymentCreatedEvent) {
        for (item in event.cartItems) {
            val skuId = item.skuId ?: continue
            if(productRepository.findBySkuId(skuId) == null) throw ProductNotFoundException("Product not found: $skuId")
            // Atomic operation: decrement only if stock >= quantity. Avoids race conditions
            val modifiedCount = decrementStock(
                skuId = skuId,
                requiredStock = item.quantity,
            )
            val sufficientStock = modifiedCount > 0
            val productUpdatedEvent = ProductUpdatedEvent(
                orderId = event.orderId,
                customerEmail = event.customerEmail,
                skuId = skuId,
                quantity = item.quantity,
                sufficientStock = sufficientStock
            )
            kafkaTemplate.send(KafkaTopic.PRODUCTS, event.orderId.toString(), productUpdatedEvent)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        logger.info("Success Kafka event sent. Service: Product. Order id ${event.orderId}")
                    } else {
                        logger.error("Failure Kafka event sent. Service: Product. Order id ${event.orderId}", ex)
                    }
                }
        }
    }
}