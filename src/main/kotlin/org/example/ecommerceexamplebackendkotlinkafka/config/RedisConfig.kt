package org.example.ecommerceexamplebackendkotlinkafka.config

import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {
    // Using Spring Boot auto-configuration for Redis caching
    // Configuration is in application.properties:
    // - spring.cache.type=redis
    // - spring.cache.redis.time-to-live=600000 (10 minutes)
}
