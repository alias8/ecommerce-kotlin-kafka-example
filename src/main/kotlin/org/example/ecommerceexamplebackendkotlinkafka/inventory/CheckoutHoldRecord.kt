package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

// Per-checkout record: the sku -> quantity lines a single checkout claimed.
//
// Redis shape (one hash per checkout hold, field = skuId, value = quantity):
//   "stock:checkout:{reservationId}" -> { [skuId: string]: quantity }
//
// e.g. "stock:checkout:abc-123" -> { "SKU1": 3, "SKU2": 1 }
@Component
class CheckoutHoldRecord(private val redisTemplate: StringRedisTemplate) {

    fun quantityOf(reservationId: String, skuId: String): Int =
        redisTemplate.opsForHash<String, String>().get(key(reservationId), skuId)?.toIntOrNull() ?: 0

    fun put(reservationId: String, skuId: String, quantity: Int) {
        redisTemplate.opsForHash<String, String>().put(key(reservationId), skuId, quantity.toString())
    }

    fun expireAfter(reservationId: String, ttl: Duration) {
        redisTemplate.expire(key(reservationId), ttl)
    }

    fun skuIds(reservationId: String): Set<String> =
        redisTemplate.opsForHash<String, String>().keys(key(reservationId))

    fun delete(reservationId: String) {
        redisTemplate.delete(key(reservationId))
    }

    private fun key(reservationId: String) = "stock:checkout:$reservationId"
}
