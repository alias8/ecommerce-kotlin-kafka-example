package org.example.ecommerceexamplebackendkotlinkafka.payment

import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaGroupId
import org.example.ecommerceexamplebackendkotlinkafka.order.KafkaTopic
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    }

    init {
        logger.info("PaymentService bean created!")
    }

    @KafkaListener(topics = [KafkaTopic.ORDERS], groupId = KafkaGroupId.PAYMENT_SERVICE)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        /*
        * 1. Process payment, try to query payment API
        * 2. Store success/fail result in Payment database
        * 3. Produce new event saying result of payment
        * */

        val success = Math.random() > 0.1 // Simulate API call, 90% success rate
        val newPayment = Payment().apply {
            orderId = event.orderId
            totalPrice = event.totalPrice
            paymentToken = event.paymentToken
            this.success = success
        }
        paymentRepository.save(newPayment)

        val paymentCreatedEvent = PaymentCreatedEvent(
            success = success,
            cartItems = event.cartItems,
            orderId = event.orderId,
            customerEmail = event.customerEmail
        )
        kafkaTemplate.send(KafkaTopic.PAYMENTS, if (success) "success" else "fail", paymentCreatedEvent)
    }
}