package org.example.ecommerceexamplebackendkotlinkafka.report

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface ReportJobRepository : JpaRepository<ReportJob, Long> {

    /**
     * Idempotency: claim the job with ONE conditional UPDATE. Returns 1 if this worker
     * won the claim, 0 if it must skip. The database decides atomically, so two workers
     * holding the same message can't both win.
     *
     * Claimable when:
     * - PENDING (never started, or released after a failed attempt), or
     * - PROCESSING but claimed before [staleBefore]: the previous worker died mid-job.
     * Not claimable when COMPLETED (a duplicate delivery) or claimed recently (another
     * worker is on it now).
     */
    @Modifying
    @Transactional
    @Query(
        """
        UPDATE ReportJob j SET j.status = :processing, j.claimedAt = :now
        WHERE j.id = :id
          AND (j.status = :pending OR (j.status = :processing AND j.claimedAt < :staleBefore))
        """
    )
    fun claim(
        @Param("id") id: Long,
        @Param("now") now: Instant,
        @Param("staleBefore") staleBefore: Instant,
        @Param("pending") pending: ReportStatus = ReportStatus.PENDING,
        @Param("processing") processing: ReportStatus = ReportStatus.PROCESSING,
    ): Int

    /** Give the claim back after a failed attempt, so the retry can claim it again. */
    @Modifying
    @Transactional
    @Query("UPDATE ReportJob j SET j.status = :pending, j.claimedAt = null WHERE j.id = :id AND j.status = :processing")
    fun release(
        @Param("id") id: Long,
        @Param("pending") pending: ReportStatus = ReportStatus.PENDING,
        @Param("processing") processing: ReportStatus = ReportStatus.PROCESSING,
    ): Int
}
