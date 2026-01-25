package org.example.ecommerceexamplebackendkotlinkafka.shipping

import jakarta.transaction.Transactional
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderStatus
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ShippingService(
    private val shippingRepository: ShippingRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ShippingService::class.java)
    }

    init {
        logger.info("ShippingService bean created!")
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopic.PRODUCTS],
        groupId = KafkaGroupId.SHIPPING_SERVICE,
    )
    fun handleProductUpdatedEvent(event: ProductUpdatedEvent) {
        if (event.sufficientStock) {
            // Create a shipping record
            val shippingEvent = Shipping().apply {
                orderId = event.orderId
                status = OrderStatus.SHIPPED
                skuId = event.skuId
                quantity = event.quantity
            }
            shippingRepository.save(shippingEvent)
        } else {
            // Need to order more inventory
        }

        // Emit Kafka event
    }
}