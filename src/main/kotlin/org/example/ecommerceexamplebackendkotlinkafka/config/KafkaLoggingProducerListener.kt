package org.example.ecommerceexamplebackendkotlinkafka.config

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRelatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.ProducerListener
import org.springframework.stereotype.Component

@Component
class KafkaLoggingProducerListener : ProducerListener<String, Any> {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getLoggerTemplateString(success: Boolean): String =
        "Kafka event send ${if (success) "success" else "failure"}. orderId={} topic={} key={}"

    private fun getOrderId(producerRecord: ProducerRecord<String, Any>): String =
        (producerRecord.value() as? OrderRelatedEvent)?.orderId?.toString() ?: "unknown orderId"


    override fun onSuccess(producerRecord: ProducerRecord<String, Any>, recordMetadata: RecordMetadata) {
        logger.info(
            getLoggerTemplateString(true),
            getOrderId(producerRecord),
            producerRecord.topic(),
            producerRecord.key()
        )
    }

    override fun onError(
        producerRecord: ProducerRecord<String, Any>,
        recordMetadata: RecordMetadata?,
        exception: Exception
    ) {
        logger.error(
            getLoggerTemplateString(false),
            getOrderId(producerRecord),
            producerRecord.topic(),
            producerRecord.key(),
            exception
        )
    }
}
