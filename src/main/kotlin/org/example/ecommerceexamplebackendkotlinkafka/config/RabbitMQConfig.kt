package org.example.ecommerceexamplebackendkotlinkafka.config

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    companion object {
        // === SHIPPING NOTIFICATIONS ===
        const val SHIPPING_QUEUE = "shipping-notifications"
        const val SHIPPING_ROUTING_KEY = "notification.shipping"
        const val SHIPPING_DLQ = "shipping-notifications-dlq"

        // === REPORT JOBS ===
        const val REPORT_QUEUE = "report-jobs"
        const val REPORT_ROUTING_KEY = "report.generate"
        const val REPORT_DLQ = "report-jobs-dlq"

        // === SHARED ===
        const val EXCHANGE = "app-exchange"  // one exchange can route to multiple queues
        const val DLX = "dead-letter-exchange"  // one DLX can handle all dead letters
    }

    // === EXCHANGE (shared by all queues) ===
    @Bean
    fun exchange(): TopicExchange = TopicExchange(EXCHANGE)

    @Bean
    fun dlx(): DirectExchange = DirectExchange(DLX)

    @Bean
    fun shipping(exchange: TopicExchange, dlx: DirectExchange): Declarables {
        val queue = QueueBuilder.durable(SHIPPING_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .build()
        val dlq = Queue("$SHIPPING_QUEUE-dlq", true)

        return Declarables(
            queue,
            dlq,
            BindingBuilder.bind(queue).to(exchange).with(SHIPPING_ROUTING_KEY),
            BindingBuilder.bind(dlq).to(dlx).with(SHIPPING_ROUTING_KEY)
        )
    }

    @Bean
    fun reportJob(exchange: TopicExchange, dlx: DirectExchange): Declarables {
        val queue = QueueBuilder.durable(REPORT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .build()
        val dlq = Queue("$REPORT_QUEUE-dlq", true)

        return Declarables(
            queue,
            dlq,
            BindingBuilder.bind(queue).to(exchange).with(REPORT_ROUTING_KEY),
            BindingBuilder.bind(dlq).to(dlx).with(REPORT_ROUTING_KEY)
        )
    }

    // === CONVERTER ===
    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()
}