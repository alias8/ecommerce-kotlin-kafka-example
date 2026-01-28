package org.example.ecommerceexamplebackendkotlinkafka.inventory

import jakarta.transaction.Transactional
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.payment.PaymentCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(InventoryService::class.java)
    }

    init {
        logger.info("InventoryService bean created!")
    }

    @Transactional
    @KafkaListener(
        topics = [KafkaTopic.PAYMENTS],
        groupId = KafkaGroupId.INVENTORY_SERVICE,
        containerFactory = "filterSuccessMessagesKafkaListenerContainerFactory"
    )
    fun handlePaymentCreated(event: PaymentCreatedEvent) {
        for (item in event.cartItems) {
            val skuId = item.skuId ?: continue
            if (inventoryRepository.findBySkuId(skuId) == null) {
                logger.error("Product not found: $skuId")
                continue
            }
            // Atomic operation: decrement only if stock >= quantity. Avoids race conditions
            val modifiedCount = inventoryRepository.decrementStock(skuId, item.quantity)
            val sufficientStock = modifiedCount == 1
            val productUpdatedEvent = InventoryUpdatedEvent(
                orderId = event.orderId,
                customerEmail = event.customerEmail,
                skuId = skuId,
                quantity = item.quantity,
                sufficientStock = sufficientStock
            )
            kafkaTemplate.send(KafkaTopic.INVENTORY, event.orderId.toString(), productUpdatedEvent)
        }
    }
}