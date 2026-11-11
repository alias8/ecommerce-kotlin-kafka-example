package org.example.ecommerceexamplebackendkotlinkafka.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.ecommerceexamplebackendkotlinkafka.outbox.OutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The request side of report jobs: runs in the web process.
 * The slow work lives in [ReportWorker], which runs in a separate worker process.
 */
@Service
class ReportJobService(
    private val reportJobRepository: ReportJobRepository,
    private val outboxRepository: OutboxRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    fun getReport(id: Long): ReportJob? {
        return reportJobRepository.findById(id).orElse(null)
    }

    /*
    * The user calls an endpoint to create a report, which takes ~30 seconds to generate.
    * We save a PENDING job, queue it, and return the id straight away.
    * The user polls GET /report/{id} to see the status.
    *
    * Before: save the job, then rabbitTemplate.convertAndSend(...). Two systems, no shared
    * transaction: a crash between the two lines left a PENDING job that never ran.
    * Now: the job row and the outbox row commit together in one Postgres transaction.
    * OutboxRelay sends the message to RabbitMQ afterwards.
    * */
    @Transactional
    fun createReport(request: ReportRequest): Long? {
        val reportJob = reportJobRepository.save(ReportJob().apply {
            customerEmail = request.customerEmail
        })
        val jobKey = "report:${reportJob.id}"
        val message = ReportNotificationMessage(
            jobKey = jobKey,
            reportJobId = reportJob.id,
            customerEmail = request.customerEmail,
        )
        outboxRepository.insert(jobKey, type = "report", payload = objectMapper.writeValueAsString(message))
        return reportJob.id
    }
}
