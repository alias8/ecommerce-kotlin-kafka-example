package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

// Per-SKU index: which checkout holds currently claim units of this SKU, and until when.
//
// Redis shape (one sorted set per SKU, member = reservationId, score = expiry):
//   "stock:checkout-hold:{skuId}" -> { [reservationId: string]: expiresAtEpochMs }
//
// e.g. "stock:checkout-hold:SKU1" -> { "abc-123": 1784580832000, "xyz-999": 1784580900000 }
@Component
class CheckoutHoldIndex(private val redisTemplate: StringRedisTemplate) {

    fun purgeExpired(skuId: String, now: Long) {
        redisTemplate.opsForZSet().removeRangeByScore(key(skuId), Double.NEGATIVE_INFINITY, now.toDouble())
    }

    fun activeReservationIds(skuId: String): Set<String> =
        redisTemplate.opsForZSet().range(key(skuId), 0, -1) ?: emptySet()

    fun recordHold(skuId: String, reservationId: String, expiresAt: Long) {
        redisTemplate.opsForZSet().add(key(skuId), reservationId, expiresAt.toDouble())
    }

    fun releaseHold(skuId: String, reservationId: String) {
        redisTemplate.opsForZSet().remove(key(skuId), reservationId)
    }

    private fun key(skuId: String) = "stock:checkout-hold:$skuId"
}
