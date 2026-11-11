package org.example.ecommerceexamplebackendkotlinkafka.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.SqsHeaders
import org.example.ecommerceexamplebackendkotlinkafka.messaging.SqsMessagePublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

/**
 * SQS consumer for report jobs (worker process, app.queue.transport=sqs).
 *
 * Differences from RabbitMQ:
 * - SQS never pushes. The container LONG-POLLS: ReceiveMessage waits up to pollTimeoutSeconds
 *   for messages, in a loop, on background threads.
 * - Receiving HIDES the message for messageVisibilitySeconds; it isn't deleted yet.
 *   Returning normally -> the container DELETES it. Throwing -> nothing happens; when the
 *   visibility timeout runs out, the message reappears and is received again.
 *   (So a worker crash also means a wait of up to the visibility timeout, not an instant redelivery.)
 * - Visibility timeout must be longer than the job (30 s build + upload here), or a second worker
 *   gets the message while the first is still working. Very long jobs extend it as they go
 *   (a heartbeat: ChangeMessageVisibility).
 * - Retries and the DLQ are SQS-side, set on the queue, not in the app: a redrive policy
 *   {"deadLetterTargetArn": "...report-jobs-dlq", "maxReceiveCount": 5} moves the message to the DLQ
 *   on its 5th failed receive. SQS counts the attempts (ApproximateReceiveCount).
 * - Alarm on ApproximateAgeOfOldestMessage (main queue) and on DLQ depth > 0.
 */
@Component
@ConditionalOnProperty("app.worker.enabled", havingValue = "true")
@ConditionalOnProperty("app.queue.transport", havingValue = "sqs")
class SqsReportListener(
    private val reportWorker: ReportWorker,
    private val reportJobRepository: ReportJobRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SqsReportListener::class.java)
        private val objectMapper = jacksonObjectMapper()
        private const val MAX_RECEIVE_COUNT = 5 // must match the queue's redrive policy
    }

    @SqsListener(
        queueNames = [SqsMessagePublisher.REPORT_QUEUE],
        pollTimeoutSeconds = "20",          // long polling: wait up to 20 s for a message
        messageVisibilitySeconds = "120",   // > job duration
        maxConcurrentMessages = "5",
    )
    fun onMessage(
        body: String,
        @Header(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT) receiveCount: String,
    ) {
        val message = objectMapper.readValue<ReportNotificationMessage>(body)
        try {
            reportWorker.processReport(message)
        } catch (e: Exception) {
            // On the last attempt SQS will dead-letter the message instead of redelivering it,
            // so this is our last chance to tell the user (status FAILED instead of PENDING forever).
            if (receiveCount.toInt() >= MAX_RECEIVE_COUNT) {
                logger.error("{} failed on final attempt {}; marking FAILED", message.jobKey, receiveCount, e)
                message.reportJobId?.let { id ->
                    reportJobRepository.findById(id).ifPresent { job ->
                        job.status = ReportStatus.FAILED
                        reportJobRepository.save(job)
                    }
                }
            }
            throw e // not deleted -> visible again after the timeout, or DLQ after maxReceiveCount
        }
    }
}
