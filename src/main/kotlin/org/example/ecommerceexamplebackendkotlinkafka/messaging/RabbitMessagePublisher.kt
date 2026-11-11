package org.example.ecommerceexamplebackendkotlinkafka.messaging

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * RabbitMQ: publish to an EXCHANGE with a routing key; bindings decide which queue(s) get it
 * (one message can fan out to several queues). The broker PUSHES messages to consumers.
 */
@Component
@ConditionalOnProperty("app.queue.transport", havingValue = "rabbit", matchIfMissing = true)
class RabbitMessagePublisher(private val rabbitTemplate: RabbitTemplate) : MessagePublisher {
    private val routingKeys = mapOf(
        "report" to RabbitMQConfig.REPORT_ROUTING_KEY,
    )

    override fun publish(type: String, jobKey: String, payloadJson: String) {
        val routingKey = routingKeys[type] ?: throw IllegalStateException("No routing key for type $type")
        // The payload is already JSON, so send it as-is rather than re-serialising.
        val message = MessageBuilder.withBody(payloadJson.toByteArray())
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setMessageId(jobKey)   // the job key travels with the message
            .build()
        // For a guarantee the broker stored it before the outbox row is marked sent,
        // enable publisher confirms (spring.rabbitmq.publisher-confirm-type=correlated).
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, routingKey, message)
    }
}
