package org.example.ecommerceexamplebackendkotlinkafka.report

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * The slow side of report jobs: consumes the report queue.
 *
 * Only created when app.worker.enabled=true, so the same JAR can run as:
 *   web:    --app.worker.enabled=false                  (serves HTTP, never consumes)
 *   worker: --app.worker.enabled=true \
 *           --spring.main.web-application-type=none     (consumes, no HTTP server)
 * Two deployments from one image, scaled separately: web on request rate,
 * workers on queue age. A crashing or slow job can't starve the API.
 * Defaults to true (application.properties) so a single local process still does both.
 */
@Component
@ConditionalOnProperty("app.worker.enabled", havingValue = "true")
class ReportWorker(
    private val reportJobRepository: ReportJobRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReportWorker::class.java)

        // Must be longer than the slowest real job, or a live worker's job gets taken over.
        // (RabbitMQ only redelivers if the consumer's connection drops or it exceeds the
        // broker's ack timeout, 30 min by default; SQS would redeliver after the visibility timeout.)
        private val CLAIM_LEASE: Duration = Duration.ofMinutes(10)
    }

    // Spring starts a listener container: threads holding a connection to RabbitMQ.
    // RabbitMQ pushes each message here; returning normally acks it (RabbitMQ deletes it),
    // throwing triggers the retry policy, then the dead-letter queue (see RabbitMQConfig).
    @RabbitListener(queues = [RabbitMQConfig.REPORT_QUEUE])
    fun processReport(message: ReportNotificationMessage) {
        val reportJobId = message.reportJobId ?: throw RuntimeException("reportJobId is null")
        val now = Instant.now()

        // At-least-once delivery (and the outbox relay) can hand us the same job twice.
        // Only the worker that wins the claim does the work; everyone else acks and moves on.
        if (reportJobRepository.claim(reportJobId, now, staleBefore = now.minus(CLAIM_LEASE)) == 0) {
            logger.info("Skipping {}: already completed or being processed", message.jobKey)
            return
        }
        logger.info("Processing {}", message.jobKey)

        try {
            Thread.sleep(30000) // the slow work
        } catch (e: Exception) {
            // Give the claim back so the retry can take it; otherwise the retry would see
            // PROCESSING, skip, ack the message, and the job would be stuck forever.
            reportJobRepository.release(reportJobId)
            throw e
        }

        val reportJob = reportJobRepository.findById(reportJobId).orElseThrow()
        reportJob.status = ReportStatus.COMPLETED
        reportJob.completedAt = Instant.now()
        reportJobRepository.save(reportJob)
    }
}
