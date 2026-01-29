package org.example.ecommerceexamplebackendkotlinkafka.report

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/report")
class ReportController(private val reportService: ReportJobService) {

    @PostMapping
    fun createReport(@Valid @RequestBody request: ReportRequest): ResponseEntity<Long> {
        val createReportJob = reportService.createReport(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createReportJob)
    }

    @GetMapping("/{id}")
    fun getReport(@PathVariable id: Long): ResponseEntity<ReportJob> {
        val report = reportService.getReport(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(report)
    }
}