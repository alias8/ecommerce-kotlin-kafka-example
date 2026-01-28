package org.example.ecommerceexamplebackendkotlinkafka.config

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.ProducerListener
import org.springframework.stereotype.Component

@Component
class KafkaLoggingProducerListener : ProducerListener<String, Any> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onSuccess(producerRecord: ProducerRecord<String, Any>, recordMetadata: RecordMetadata) {
        logger.info("Kafka event sent. topic={} key={}", producerRecord.topic(), producerRecord.key())
    }

    override fun onError(
        producerRecord: ProducerRecord<String, Any>,
        recordMetadata: RecordMetadata?,
        exception: Exception
    ) {
        logger.error(
            "Failed to send Kafka event. topic={} key={}",
            producerRecord.topic(),
            producerRecord.key(),
            exception
        )
    }
}
