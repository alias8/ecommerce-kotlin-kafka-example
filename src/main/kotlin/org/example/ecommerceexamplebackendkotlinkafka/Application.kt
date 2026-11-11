package org.example.ecommerceexamplebackendkotlinkafka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableCaching
@EnableScheduling // for OutboxRelay
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}