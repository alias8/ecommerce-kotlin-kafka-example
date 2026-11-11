package org.example.ecommerceexamplebackendkotlinkafka.report

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * RabbitMQ consumer for report jobs (worker process, app.queue.transport=rabbit).
 *
 * Spring starts a listener container: threads holding an open connection to RabbitMQ.
 * The broker PUSHES each message here. Returning normally ACKs it (RabbitMQ deletes it).
 * Throwing: Spring's retry policy re-runs it in-process (application.properties: 5 attempts,
 * backoff 1 s x2 up to 10 s), then ReportFailureRecoverer marks the job FAILED and rejects it,
 * and the dead-letter exchange routes it to report-jobs-dlq (RabbitMQConfig).
 * If the worker dies mid-job, the connection drops and RabbitMQ redelivers straight away.
 */
@Component
@ConditionalOnProperty("app.worker.enabled", havingValue = "true")
@ConditionalOnProperty("app.queue.transport", havingValue = "rabbit", matchIfMissing = true)
class RabbitReportListener(private val reportWorker: ReportWorker) {

    @RabbitListener(queues = [RabbitMQConfig.REPORT_QUEUE])
    fun onMessage(message: ReportNotificationMessage) {
        reportWorker.processReport(message)
    }
}
