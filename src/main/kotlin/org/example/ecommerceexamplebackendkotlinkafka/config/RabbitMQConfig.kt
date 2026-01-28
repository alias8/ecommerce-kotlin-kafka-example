package org.example.ecommerceexamplebackendkotlinkafka.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    companion object {
        const val SHIPPING_NOTIFICATIONS_QUEUE_NAME = "shipping-notifications"
        const val NOTIFICATIONS_EXCHANGE = "notifications-exchange"
        const val NOTIFICATION_SHIPPING_ROUTING_KEY = "notification.shipping"
    }

    // Declare the queue as a Spring bean
    @Bean
    fun queue(): Queue {
        // The queue is durable (survives a broker restart)
        return Queue(SHIPPING_NOTIFICATIONS_QUEUE_NAME, true)
    }

    // Declare the exchange as a Spring bean (e.g., Topic exchange)
    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(NOTIFICATIONS_EXCHANGE)
    }

    // Bind the queue to the exchange with a routing key
    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(queue)
            .to(exchange)
            .with(NOTIFICATION_SHIPPING_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()
}