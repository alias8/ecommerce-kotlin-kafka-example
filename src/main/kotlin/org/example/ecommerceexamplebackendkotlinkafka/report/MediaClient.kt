package org.example.ecommerceexamplebackendkotlinkafka.report

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.UUID

/**
 * Uploads finished report files to a (pretend) media/file service.
 *
 * TIMEOUTS: outbound HTTP clients often default to waiting forever (HttpURLConnection,
 * which backs RestTemplate/RestClient's simple factory, uses 0 = infinite). A worker with no
 * timeout hangs on a stuck downstream, takes no new messages, and nothing errors: the DLQ
 * stays empty and only the queue's oldest-message age shows the outage. With timeouts the
 * hang becomes an exception, so retries and the DLQ work as designed.
 */
@Component
class MediaClient(
    @Value("\${app.media.base-url:http://localhost:9999}") baseUrl: String,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))   // can't even reach it
                    .build()
            ).apply {
                // No response (headers) within 10 s -> HttpTimeoutException.
                // Base it on the service's measured latency, well above its p99.
                // Note: a server trickling bytes slowly can still outlast this; a hard
                // overall deadline needs an extra wrapper (e.g. a future with a timeout).
                setReadTimeout(Duration.ofSeconds(10))
            }
        )
        .build()

    /**
     * IDEMPOTENT UPLOAD: the file id is derived from the job key, so every retry of the same
     * job writes to the same file (PUT = create or replace) instead of creating a new one.
     * Matters because a timeout doesn't mean the upload failed: it may have succeeded and
     * only the reply was lost, and the retry will upload again.
     * (nameUUIDFromBytes = name-based v3 UUID; same input, same UUID.)
     */
    fun upload(jobKey: String, content: ByteArray, contentType: MediaType): UUID {
        val fileId = UUID.nameUUIDFromBytes(jobKey.toByteArray())
        restClient.put()
            .uri("/file/{fileId}", fileId)
            .contentType(contentType)
            .body(content)
            .retrieve()
            .toBodilessEntity()
        return fileId
    }
}
