package org.example.ecommerceexamplebackendkotlinkafka.report

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

@Entity
class ReportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var customerEmail: String? = null
    var status: ReportStatus = ReportStatus.PENDING
    var requestedAt: Instant? = Instant.now()
    var completedAt: Instant? = null
    var resultPath: String? = null
}

enum class ReportStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

data class ReportRequest(
    @field:NotBlank(message = "customerEmail is required")
    @field:Email(message = "customerEmail must be a valid email address")
    val customerEmail: String?,
)

data class ReportNotificationMessage(
    val reportJobId: Long?,
    val customerEmail: String?,
)