package org.example.ecommerceexamplebackendkotlinkafka.config

import org.springframework.amqp.core.*
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

        const val DLQ_NAME = "shipping-notifications-dlq"
        const val DLX_NAME = "notifications-dlx"
    }

    @Bean
    fun queue(): Queue {
        return QueueBuilder
            .durable(SHIPPING_NOTIFICATIONS_QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .build()
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(NOTIFICATIONS_EXCHANGE)
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(queue)
            .to(exchange)
            .with(NOTIFICATION_SHIPPING_ROUTING_KEY)
    }

    @Bean
    fun dlq(): Queue {
        return Queue(DLQ_NAME, true)

    }

    @Bean
    fun dlx(): DirectExchange {
        return DirectExchange(DLX_NAME)
    }

    @Bean
    fun dlqBinding(dlq: Queue, dlx: DirectExchange): Binding {
        return BindingBuilder
            .bind(dlq)
            .to(dlx)
            .with(NOTIFICATION_SHIPPING_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()
}