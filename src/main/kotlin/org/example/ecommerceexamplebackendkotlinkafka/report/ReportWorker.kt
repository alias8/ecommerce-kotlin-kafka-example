package org.example.ecommerceexamplebackendkotlinkafka.report

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
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
    }

    // Spring starts a listener container: threads holding a connection to RabbitMQ.
    // RabbitMQ pushes each message here; returning normally acks it (RabbitMQ deletes it),
    // throwing triggers the retry policy, then the dead-letter queue (see RabbitMQConfig).
    @RabbitListener(queues = [RabbitMQConfig.REPORT_QUEUE])
    fun processReport(message: ReportNotificationMessage) {
        val reportJobId = message.reportJobId ?: throw RuntimeException("reportJobId is null")
        val reportJob = reportJobRepository.findById(reportJobId)
            .orElseThrow { RuntimeException("Unknown reportJob with id: $reportJobId") }
        logger.info("Processing report job {}", reportJobId)

        reportJob.status = ReportStatus.PROCESSING
        reportJobRepository.save(reportJob)
        Thread.sleep(30000) // the slow work
        reportJob.status = ReportStatus.COMPLETED
        reportJob.completedAt = Instant.now()
        reportJobRepository.save(reportJob)
    }
}
