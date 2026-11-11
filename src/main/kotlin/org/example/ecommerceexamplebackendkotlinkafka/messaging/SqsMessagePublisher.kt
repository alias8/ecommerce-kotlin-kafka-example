package org.example.ecommerceexamplebackendkotlinkafka.messaging

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * SQS: no exchanges or routing. You send straight to a named QUEUE (fan-out would be SNS -> several
 * SQS queues). sendMessage returns only after SQS has stored the message, so when this returns
 * without throwing, the outbox row can safely be marked sent. If it times out we can't tell whether
 * SQS stored it (and the AWS SDK retries timeouts by default), so duplicates are possible:
 * the consumer is idempotent.
 */
@Component
@ConditionalOnProperty("app.queue.transport", havingValue = "sqs")
class SqsMessagePublisher(private val sqsTemplate: SqsTemplate) : MessagePublisher {
    companion object {
        const val REPORT_QUEUE = "report-jobs"
    }

    private val queues = mapOf(
        "report" to REPORT_QUEUE,
    )

    override fun publish(type: String, jobKey: String, payloadJson: String) {
        val queue = queues[type] ?: throw IllegalStateException("No queue for type $type")
        // Standard queue: no dedup. On a FIFO queue you'd also set messageDeduplicationId(jobKey),
        // which only drops duplicate SENDS within 5 minutes; the consumer still must be idempotent.
        sqsTemplate.send { to -> to.queue(queue).payload(payloadJson).header("jobKey", jobKey) }
    }
}
