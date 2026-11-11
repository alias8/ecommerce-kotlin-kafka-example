package org.example.ecommerceexamplebackendkotlinkafka.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.stereotype.Component

/**
 * Runs once a listener's retries are used up (application.properties: 5 attempts with backoff).
 * Spring Boot plugs any MessageRecoverer bean into the listener retry policy.
 *
 * 1. For report jobs, mark the job FAILED so the user polling GET /report/{id} sees it,
 *    instead of PENDING forever.
 * 2. super.recover() rejects without requeue, so RabbitMQ routes the message through the
 *    dead-letter exchange into report-jobs-dlq, where it STAYS for inspection.
 *    (Consuming the DLQ to mark the job failed would remove the evidence.)
 *    After a fix: reset the job to PENDING and move the message back (redrive).
 *    Alarm on DLQ depth > 0.
 */
@Component
class ReportFailureRecoverer(
    private val reportJobRepository: ReportJobRepository,
) : RejectAndDontRequeueRecoverer() {
    companion object {
        private val log = LoggerFactory.getLogger(ReportFailureRecoverer::class.java)
        private val objectMapper = jacksonObjectMapper()
    }

    override fun recover(message: Message, cause: Throwable) {
        if (message.messageProperties.consumerQueue == RabbitMQConfig.REPORT_QUEUE) {
            runCatching {
                val payload = objectMapper.readValue<ReportNotificationMessage>(message.body)
                log.error("{} failed after retries; marking FAILED and dead-lettering", payload.jobKey, cause)
                payload.reportJobId?.let { id ->
                    reportJobRepository.findById(id).ifPresent { job ->
                        job.status = ReportStatus.FAILED
                        reportJobRepository.save(job)
                    }
                }
            }.onFailure { log.error("Could not mark report job FAILED", it) }
        }
        super.recover(message, cause) // reject -> dead-letter exchange -> DLQ
    }
}
