package org.example.ecommerceexamplebackendkotlinkafka.inventory

import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import redis.embedded.RedisServer

class StockReservationServiceTest {

    companion object {
        private val port = 16400 + (Math.random() * 1000).toInt()
        private lateinit var redisServer: RedisServer
        private lateinit var connectionFactory: LettuceConnectionFactory

        @JvmStatic
        @BeforeAll
        fun startRedis() {
            redisServer = RedisServer(port)
            redisServer.start()
            connectionFactory = LettuceConnectionFactory("localhost", port)
            connectionFactory.afterPropertiesSet()
        }

        @JvmStatic
        @AfterAll
        fun stopRedis() {
            connectionFactory.destroy()
            redisServer.stop()
        }
    }

    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var checkoutHoldAnSku: CheckoutHoldAnSku
    private lateinit var checkoutHoldId: CheckoutHoldId
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var service: StockReservationService

    @BeforeEach
    fun setUp() {
        redisTemplate = StringRedisTemplate(connectionFactory)
        redisTemplate.afterPropertiesSet()
        redisTemplate.connectionFactory!!.connection.use { it.serverCommands().flushDb() }

        checkoutHoldAnSku = CheckoutHoldAnSku(redisTemplate)
        checkoutHoldId = CheckoutHoldId(redisTemplate)
        inventoryRepository = mock()
        service = StockReservationService(checkoutHoldAnSku, checkoutHoldId, inventoryRepository)
    }

    private fun stockOf(skuId: String, level: Int) {
        whenever(inventoryRepository.findBySkuId(skuId)).thenReturn(Inventory().apply {
            this.skuId = skuId
            stockLevel = level
        })
    }

    @Test
    fun `reserves stock when enough is available`() {
        stockOf("SKU1", 10)

        val reservationId = service.reserve(listOf(CartItemRequest("SKU1", 4)))

        assertTrue(reservationId.isNotBlank())
    }

    @Test
    fun `rejects a reservation that exceeds available stock`() {
        stockOf("SKU1", 3)

        assertThrows(InsufficientStockException::class.java) {
            service.reserve(listOf(CartItemRequest("SKU1", 4)))
        }
    }

    @Test
    fun `a held reservation reduces what a second checkout can reserve`() {
        stockOf("SKU1", 10)

        service.reserve(listOf(CartItemRequest("SKU1", 7)))

        val ex = assertThrows(InsufficientStockException::class.java) {
            service.reserve(listOf(CartItemRequest("SKU1", 4)))
        }
        assertEquals(listOf("SKU1"), ex.skuIds)

        // 3 units remain available (10 - 7 held), so this fits
        val reservationId = service.reserve(listOf(CartItemRequest("SKU1", 3)))
        assertTrue(reservationId.isNotBlank())
    }

    @Test
    fun `expired reservations no longer count against availability`() {
        stockOf("SKU1", 10)

        val expiredReservationId = "expired-reservation"
        checkoutHoldAnSku.recordHold("SKU1", expiredReservationId, System.currentTimeMillis() - 60_000)
        checkoutHoldId.put(expiredReservationId, "SKU1", 9)

        // The stale hold above claims 9 of 10 units, but it's already expired and should be purged on read
        val reservationId = service.reserve(listOf(CartItemRequest("SKU1", 8)))
        assertTrue(reservationId.isNotBlank())
    }

    @Test
    fun `rejects only the specific items that lack stock`() {
        stockOf("SKU1", 10)
        stockOf("SKU2", 1)

        val ex = assertThrows(InsufficientStockException::class.java) {
            service.reserve(listOf(CartItemRequest("SKU1", 2), CartItemRequest("SKU2", 5)))
        }
        assertEquals(listOf("SKU2"), ex.skuIds)
    }

    @Test
    fun `release frees a hold's units back up immediately, across all skus it touched`() {
        stockOf("SKU1", 10)
        stockOf("SKU2", 5)

        val reservationId = service.reserve(listOf(CartItemRequest("SKU1", 10), CartItemRequest("SKU2", 5)))
        assertThrows(InsufficientStockException::class.java) {
            service.reserve(listOf(CartItemRequest("SKU1", 1)))
        }

        service.release(reservationId)

        // Now that the hold is released, the full stock is reservable again for both skus
        val secondReservationId = service.reserve(listOf(CartItemRequest("SKU1", 10), CartItemRequest("SKU2", 5)))
        assertTrue(secondReservationId.isNotBlank())
    }

    @Test
    fun `release is a no-op for a null reservationId`() {
        service.release(null)
    }
}
