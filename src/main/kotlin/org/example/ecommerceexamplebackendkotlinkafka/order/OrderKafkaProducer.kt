package org.example.ecommerceexamplebackendkotlinkafka.order

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, OrderCreatedEvent>
) {
    private val logger = LoggerFactory.getLogger(OrderKafkaProducer::class.java)

    companion object {
        const val TOPIC = "order_created"
    }

    fun sendOrderCreatedEvent(event: OrderCreatedEvent) {
        logger.info("Sending order_created event for order: ${event.orderId}")
        kafkaTemplate.send(TOPIC, event.orderId, event)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Order event sent successfully: ${event.orderId}, offset: ${result.recordMetadata.offset()}")
                } else {
                    logger.error("Failed to send order event: ${event.orderId}", ex)
                }
            }
    }
}