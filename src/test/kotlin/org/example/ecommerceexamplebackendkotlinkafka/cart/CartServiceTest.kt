package org.example.ecommerceexamplebackendkotlinkafka.cart

import org.example.ecommerceexamplebackendkotlinkafka.product.Product
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import redis.embedded.RedisServer

class CartServiceTest {

    companion object {
        private val port = 17400 + (Math.random() * 1000).toInt()
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
    private lateinit var productRepository: ProductRepository
    private lateinit var service: CartService

    @BeforeEach
    fun setUp() {
        redisTemplate = StringRedisTemplate(connectionFactory)
        redisTemplate.afterPropertiesSet()
        redisTemplate.connectionFactory!!.connection.use { it.serverCommands().flushDb() }

        productRepository = mock()
        service = CartService(redisTemplate, productRepository)
    }

    private fun productOf(skuId: String, unitPrice: Double) {
        whenever(productRepository.findBySkuId(skuId)).thenReturn(Product().apply {
            this.skuId = skuId
            this.unitPrice = unitPrice
        })
    }

    @Test
    fun `adding the same sku twice sums the quantity`() {
        service.addItem("user@example.com", "SKU1", 2)
        service.addItem("user@example.com", "SKU1", 3)

        assertEquals(mapOf("SKU1" to 5), service.getItems("user@example.com"))
    }

    @Test
    fun `setItemQuantity overwrites rather than adds`() {
        service.addItem("user@example.com", "SKU1", 2)
        service.setItemQuantity("user@example.com", "SKU1", 9)

        assertEquals(mapOf("SKU1" to 9), service.getItems("user@example.com"))
    }

    @Test
    fun `removeItem only removes the given sku`() {
        service.addItem("user@example.com", "SKU1", 2)
        service.addItem("user@example.com", "SKU2", 1)

        service.removeItem("user@example.com", "SKU1")

        assertEquals(mapOf("SKU2" to 1), service.getItems("user@example.com"))
    }

    @Test
    fun `clear empties the cart`() {
        service.addItem("user@example.com", "SKU1", 2)
        service.clear("user@example.com")

        assertTrue(service.getItems("user@example.com").isEmpty())
    }

    @Test
    fun `viewCart computes line totals and overall total from product prices`() {
        productOf("SKU1", 10.0)
        productOf("SKU2", 5.0)
        service.addItem("user@example.com", "SKU1", 2)
        service.addItem("user@example.com", "SKU2", 3)

        val response = service.viewCart("user@example.com")

        assertEquals(2, response.items.size)
        assertEquals(20.0, response.items.first { it.skuId == "SKU1" }.lineTotal)
        assertEquals(15.0, response.items.first { it.skuId == "SKU2" }.lineTotal)
        assertEquals(35.0, response.total)
    }

    @Test
    fun `viewCart skips items whose product no longer exists`() {
        service.addItem("user@example.com", "GONE-SKU", 1)

        val response = service.viewCart("user@example.com")

        assertTrue(response.items.isEmpty())
        assertEquals(0.0, response.total)
    }

    @Test
    fun `carts are isolated per user`() {
        service.addItem("alice@example.com", "SKU1", 1)
        service.addItem("bob@example.com", "SKU1", 2)

        assertEquals(mapOf("SKU1" to 1), service.getItems("alice@example.com"))
        assertEquals(mapOf("SKU1" to 2), service.getItems("bob@example.com"))
    }
}
