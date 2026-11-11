package org.example.ecommerceexamplebackendkotlinkafka.report

import org.example.ecommerceexamplebackendkotlinkafka.config.RabbitMQConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * The request side of report jobs: runs in the web process.
 * The slow work lives in [ReportWorker], which runs in a separate worker process.
 */
@Service
class ReportJobService(
    private val reportJobRepository: ReportJobRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    fun getReport(id: Long): ReportJob? {
        return reportJobRepository.findById(id).orElse(null)
    }

    /*
    * The user calls an endpoint to create a report, which takes ~30 seconds to generate.
    * We save a PENDING job, queue it, and return the id straight away.
    * The user polls GET /report/{id} to see the status.
    * */
    fun createReport(request: ReportRequest): Long? {
        val reportJob = ReportJob().apply {
            customerEmail = request.customerEmail
        }
        reportJobRepository.save(reportJob)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.REPORT_ROUTING_KEY,
            ReportNotificationMessage(
                reportJobId = reportJob.id,
                customerEmail = request.customerEmail
            )
        )
        return reportJob.id
    }
}
