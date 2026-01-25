package org.example.ecommerceexamplebackendkotlinkafka.product

import jakarta.transaction.Transactional
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductNotFoundException
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

    @Transactional
    @KafkaListener(
        topics = [KafkaTopic.PAYMENTS],
        groupId = KafkaGroupId.PRODUCT_SERVICE,
        containerFactory = "filterSuccessMessagesKafkaListenerContainerFactory"
    )
    fun handlePaymentCreated(event: PaymentCreatedEvent) {
        for (item in event.cartItems) {
            val skuId = item.skuId ?: continue
            val productToUpdate = productRepository.findBySkuId(skuId)
                ?: throw ProductNotFoundException("Product not found: $skuId")
            productToUpdate.stockLevel -= item.quantity
            productRepository.save(productToUpdate)

            val productUpdatedEvent = ProductUpdatedEvent(
                orderId = event.orderId,
                customerEmail = event.customerEmail,
                stockLevel = productToUpdate.stockLevel,
                skuId = skuId
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