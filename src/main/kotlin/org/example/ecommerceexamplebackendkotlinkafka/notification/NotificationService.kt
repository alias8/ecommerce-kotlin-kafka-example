package org.example.ecommerceexamplebackendkotlinkafka.notification

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.example.ecommerceexamplebackendkotlinkafka.shipping.ShippingNotificationMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    }

    init {
        logger.info("NotificationService bean created!")
    }

    @RabbitListener(queues = [RabbitMQConfig.SHIPPING_NOTIFICATIONS_QUEUE_NAME])
    fun handleNotification(message: ShippingNotificationMessage) {
        val testing = false
        if (!testing) {
            // todo: some way to send email here, when complete, do this:
            val notification = Notification().apply {
                orderId = message.orderId
                skuId = message.skuId
                quantity = message.quantity
                itemShippedEmailSent = true
            }
            notificationRepository.save(notification)
            logger.info("NotificationService received: $message")
        } else {
            throw RuntimeException("Testing DLQ!")
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.DLQ_NAME])
    fun handleDeadLetter(message: ShippingNotificationMessage) {
        logger.error("95192 Message failed processing: $message")
    }
}