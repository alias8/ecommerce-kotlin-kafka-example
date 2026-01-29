package org.example.ecommerceexamplebackendkotlinkafka.report

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReportJobService(
    private val reportJobRepository: ReportJobRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReportJobService::class.java)
    }

    init {
        logger.info("ReportService bean created!")
    }

    fun getReport(id: Long): ReportJob? {
        return reportJobRepository.findById(id).orElse(null)
    }

    /*
    * This simulates the user calling an endpoint to create a report. It takes 30 seconds to
    * generate. The user can poll the GET endpoint getReport to see the status of the report.
    * */
    fun createReport(shipping: ReportRequest): Long? {
        val reportJob = ReportJob().apply {
            customerEmail = shipping.customerEmail
        }
        reportJobRepository.save(reportJob)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.REPORT_ROUTING_KEY,
            ReportNotificationMessage(
                reportJobId = reportJob.id,
                customerEmail = shipping.customerEmail
            )
        )
        return reportJob.id

    }

    @RabbitListener(queues = [RabbitMQConfig.REPORT_QUEUE])
    fun processReport(message: ReportNotificationMessage) {
        if (message.reportJobId == null) {
            throw RuntimeException("reportJobId is null")
        }
        val reportJob = reportJobRepository.findById(message.reportJobId)
            .orElseThrow { RuntimeException("Unknown reportJob with id: $message.reportJobId") }
        reportJob.status = ReportStatus.PROCESSING
        reportJobRepository.save(reportJob)
        Thread.sleep(30000)
        reportJob.status = ReportStatus.COMPLETED
        reportJob.completedAt = Instant.now()
        reportJobRepository.save(reportJob)
    }
}