package org.example.ecommerceexamplebackendkotlinkafka.outbox

import org.example.ecommerceexamplebackendkotlinkafka.messaging.SqsMessagePublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Publishes outbox rows to SQS. Runs in the worker process.
 *
 * Failure cases:
 * - Broker down: send throws, the transaction rolls back, rows stay unsent, next tick retries.
 * - Crash after send, before markSent commits: the row is still unsent, so it is sent AGAIN.
 *   The outbox turns a lost message into a duplicate message; the consumer must be idempotent.
 */
@Component
@ConditionalOnProperty("app.worker.enabled", havingValue = "true")
class OutboxRelay(
    private val outboxRepository: OutboxRepository,
    private val messagePublisher: SqsMessagePublisher,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OutboxRelay::class.java)
        private const val BATCH_SIZE = 100
    }

    // Polls every second: up to 1 s of extra latency. CDC (e.g. Debezium on the WAL) removes the polling.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPending() {
        val rows = outboxRepository.lockUnsent(BATCH_SIZE)
        for (row in rows) {
            messagePublisher.publish(row.type, row.jobKey, row.payload)
            outboxRepository.markSent(row.id)
        }
        if (rows.isNotEmpty()) logger.info("Outbox relay published {} message(s)", rows.size)
    }
}
