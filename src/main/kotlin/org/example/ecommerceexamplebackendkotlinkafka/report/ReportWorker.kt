package org.example.ecommerceexamplebackendkotlinkafka.report

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * The slow side of report jobs. Transport-agnostic: RabbitReportListener or SqsReportListener
 * receives the message and calls [processReport].
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
    private val mediaClient: MediaClient,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReportWorker::class.java)

        // Must be longer than the slowest real job, or a live worker's job gets taken over.
        // (RabbitMQ only redelivers if the consumer's connection drops or it exceeds the
        // broker's ack timeout, 30 min by default; SQS redelivers after the visibility timeout,
        // see SqsReportListener.)
        private val CLAIM_LEASE: Duration = Duration.ofMinutes(10)
    }

    /** Returning normally = success (message deleted); throwing = failure (retried, then DLQ). */
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

        val fileId = try {
            val content = buildReport(reportJobId)
            // Times out instead of hanging if the media service is stuck (see MediaClient).
            mediaClient.upload(message.jobKey ?: "report:$reportJobId", content, MediaType.TEXT_PLAIN)
        } catch (e: Exception) {
            // Give the claim back so the retry can take it; otherwise the retry would see
            // PROCESSING, skip, ack the message, and the job would be stuck forever.
            reportJobRepository.release(reportJobId)
            throw e
        }

        val reportJob = reportJobRepository.findById(reportJobId).orElseThrow()
        reportJob.status = ReportStatus.COMPLETED
        reportJob.completedAt = Instant.now()
        reportJob.resultPath = "media:$fileId"
        reportJobRepository.save(reportJob)
    }

    // The slow part: stands in for querying orders and building a file.
    private fun buildReport(reportJobId: Long): ByteArray {
        Thread.sleep(30000)
        return "report $reportJobId generated at ${Instant.now()}\n".toByteArray()
    }
}
