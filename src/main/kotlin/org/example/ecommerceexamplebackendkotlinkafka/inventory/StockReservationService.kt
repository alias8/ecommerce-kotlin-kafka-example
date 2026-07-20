package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class StockReservationService(
    private val checkoutHoldIndex: CheckoutHoldIndex,
    private val checkoutHoldRecord: CheckoutHoldRecord,
    private val inventoryRepository: InventoryRepository
) {
    companion object {
        private val RESERVATION_TTL: Duration = Duration.ofMinutes(15)
    }

    // Holds stock for RESERVATION_TTL so concurrent checkouts can't oversell the same units.
    fun reserve(items: List<CartItemRequest>): String {
        val now = System.currentTimeMillis()
        val insufficient = items.mapNotNull { item ->
            val skuId = item.skuId!!
            if (availableStock(skuId, now) < item.quantity) skuId else null
        }
        if (insufficient.isNotEmpty()) {
            throw InsufficientStockException(insufficient)
        }

        val reservationId = UUID.randomUUID().toString()
        val expiresAt = now + RESERVATION_TTL.toMillis()
        items.forEach { item ->
            val skuId = item.skuId!!
            checkoutHoldIndex.recordHold(skuId, reservationId, expiresAt)
            checkoutHoldRecord.put(reservationId, skuId, item.quantity)
        }
        checkoutHoldRecord.expireAfter(reservationId, RESERVATION_TTL)
        return reservationId
    }

    // Releases a checkout hold early once its order is no longer relying on it (e.g. payment succeeded).
    fun release(reservationId: String?) {
        if (reservationId == null) return
        checkoutHoldRecord.skuIds(reservationId).forEach { skuId ->
            checkoutHoldIndex.releaseHold(skuId, reservationId)
        }
        checkoutHoldRecord.delete(reservationId)
    }

    // How many units of this SKU are free to reserve right now.
    private fun availableStock(skuId: String, now: Long): Int {
        checkoutHoldIndex.purgeExpired(skuId, now)
        val reservedQuantity = checkoutHoldIndex.activeReservationIds(skuId)
            .sumOf { reservationId -> checkoutHoldRecord.quantityOf(reservationId, skuId) }
        val stockLevel = inventoryRepository.findBySkuId(skuId)?.stockLevel ?: 0
        return stockLevel - reservedQuantity
    }
}
