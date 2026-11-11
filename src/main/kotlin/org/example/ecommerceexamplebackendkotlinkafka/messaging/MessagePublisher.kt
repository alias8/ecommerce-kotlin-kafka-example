package org.example.ecommerceexamplebackendkotlinkafka.messaging

/**
 * Sends one already-serialised message for a given outbox type.
 * Two implementations, picked by app.queue.transport: RabbitMessagePublisher, SqsMessagePublisher.
 * OutboxRelay only talks to this interface, so the outbox doesn't care which broker is used.
 */
interface MessagePublisher {
    fun publish(type: String, jobKey: String, payloadJson: String)
}
