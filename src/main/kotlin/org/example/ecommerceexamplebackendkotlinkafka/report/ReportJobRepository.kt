package org.example.ecommerceexamplebackendkotlinkafka.report

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportJobRepository : JpaRepository<ReportJob, Long>