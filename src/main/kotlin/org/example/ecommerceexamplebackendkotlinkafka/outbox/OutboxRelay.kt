package org.example.ecommerceexamplebackendkotlinkafka.outbox

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Publishes outbox rows to RabbitMQ. Runs in the worker process.
 *
 * Failure cases:
 * - Broker down: send throws, the transaction rolls back, rows stay unsent, next tick retries.
 * - Crash after send, before markSent commits: the row is still unsent, so it is sent AGAIN.
 *   The outbox turns a lost message into a duplicate message; the consumer must be idempotent.
 * - For a real guarantee that the broker stored the message before we mark it sent, enable
 *   publisher confirms (spring.rabbitmq.publisher-confirm-type=correlated) and wait for the ack.
 */
@Component
@ConditionalOnProperty("app.worker.enabled", havingValue = "true")
class OutboxRelay(
    private val outboxRepository: OutboxRepository,
    private val rabbitTemplate: RabbitTemplate,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OutboxRelay::class.java)
        private const val BATCH_SIZE = 100

        // outbox.type -> routing key on the shared exchange
        private val routingKeys = mapOf(
            "report" to RabbitMQConfig.REPORT_ROUTING_KEY,
        )
    }

    // Polls every second: up to 1 s of extra latency. CDC (e.g. Debezium on the WAL) removes the polling.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPending() {
        val rows = outboxRepository.lockUnsent(BATCH_SIZE)
        for (row in rows) {
            val routingKey = routingKeys[row.type]
                ?: throw IllegalStateException("No routing key for outbox type ${row.type}")

            // The payload is already JSON, so send it as-is rather than re-serialising.
            val message = MessageBuilder.withBody(row.payload.toByteArray())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(row.jobKey)   // the job key travels with the message
                .build()
            rabbitTemplate.send(RabbitMQConfig.EXCHANGE, routingKey, message)
            outboxRepository.markSent(row.id)
        }
        if (rows.isNotEmpty()) logger.info("Outbox relay published {} message(s)", rows.size)
    }
}
