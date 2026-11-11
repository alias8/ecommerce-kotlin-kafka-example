package org.example.ecommerceexamplebackendkotlinkafka.outbox

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

data class OutboxRow(val id: Long, val jobKey: String, val type: String, val payload: String)

/**
 * Plain SQL rather than JPA: the relay needs FOR UPDATE SKIP LOCKED, which is clearer as SQL.
 */
@Repository
class OutboxRepository(private val jdbc: JdbcTemplate) {

    /** Call inside the same @Transactional as the business write. */
    fun insert(jobKey: String, type: String, payload: String) {
        jdbc.update(
            "INSERT INTO outbox (job_key, type, payload) VALUES (?, ?, ?)",
            jobKey, type, payload
        )
    }

    /**
     * Locks up to [limit] unsent rows for the current transaction.
     * SKIP LOCKED: rows another relay instance has locked are skipped, not waited on,
     * so several worker instances can run the relay without sending the same row twice
     * at the same time.
     */
    fun lockUnsent(limit: Int): List<OutboxRow> =
        jdbc.query(
            """
            SELECT id, job_key, type, payload FROM outbox
            WHERE sent_at IS NULL
            ORDER BY id
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """.trimIndent(),
            { rs, _ -> OutboxRow(rs.getLong("id"), rs.getString("job_key"), rs.getString("type"), rs.getString("payload")) },
            limit
        )

    fun markSent(id: Long) {
        jdbc.update("UPDATE outbox SET sent_at = now() WHERE id = ?", id)
    }
}
