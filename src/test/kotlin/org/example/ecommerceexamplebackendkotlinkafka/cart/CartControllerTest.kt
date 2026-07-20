package org.example.ecommerceexamplebackendkotlinkafka.cart

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.ecommerceexamplebackendkotlinkafka.auth.JwtFilter
import org.example.ecommerceexamplebackendkotlinkafka.auth.JwtUtil
import org.example.ecommerceexamplebackendkotlinkafka.config.SecurityConfig
import org.example.ecommerceexamplebackendkotlinkafka.inventory.InsufficientStockException
import org.example.ecommerceexamplebackendkotlinkafka.inventory.StockReservationService
import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderFromStore
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@WebMvcTest(CartController::class)
@EnableWebSecurity
@Import(SecurityConfig::class, JwtFilter::class, JwtUtil::class)
@TestPropertySource(properties = [
    "jwt.secret=test-secret-test-secret-test-secret-test-secret",
    "jwt.expiration-ms=60000",
    "cookie.secure=false"
])
class CartControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var cartService: CartService

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var stockReservationService: StockReservationService

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var orderService: OrderService

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    lateinit var cacheManager: org.springframework.cache.CacheManager

    @Autowired
    lateinit var jwtUtil: JwtUtil

    val objectMapper = ObjectMapper()

    private fun authHeader(email: String = "user@example.com") = "Bearer " + jwtUtil.generate(email)

    @Test
    fun `cart endpoints require authentication`() {
        mockMvc.get("/cart").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `getCart returns the caller's cart`() {
        whenever(cartService.viewCart("user@example.com")).thenReturn(
            CartResponse(listOf(CartItemView("SKU1", 2, 10.0, 20.0)), 20.0)
        )

        mockMvc.get("/cart") {
            header("Authorization", authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(20.0) }
        }
    }

    @Test
    fun `addItem adds to the caller's cart and returns the updated view`() {
        whenever(cartService.viewCart("user@example.com")).thenReturn(CartResponse(emptyList(), 0.0))

        mockMvc.post("/cart/items") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddCartItemRequest("SKU1", 3))
        }.andExpect {
            status { isOk() }
        }

        verify(cartService).addItem("user@example.com", "SKU1", 3)
    }

    @Test
    fun `addItem rejects an invalid payload`() {
        mockMvc.post("/cart/items") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AddCartItemRequest("", 0))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `updateItem sets the exact quantity`() {
        whenever(cartService.viewCart("user@example.com")).thenReturn(CartResponse(emptyList(), 0.0))

        mockMvc.put("/cart/items/SKU1") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateCartItemRequest(5))
        }.andExpect {
            status { isOk() }
        }

        verify(cartService).setItemQuantity("user@example.com", "SKU1", 5)
    }

    @Test
    fun `removeItem removes the sku from the caller's cart`() {
        whenever(cartService.viewCart("user@example.com")).thenReturn(CartResponse(emptyList(), 0.0))

        mockMvc.delete("/cart/items/SKU1") {
            header("Authorization", authHeader())
        }.andExpect {
            status { isOk() }
        }

        verify(cartService).removeItem("user@example.com", "SKU1")
    }

    @Test
    fun `checkout reserves stock, creates an order, and clears the cart`() {
        whenever(cartService.getItems("user@example.com")).thenReturn(mapOf("SKU1" to 2))
        whenever(stockReservationService.reserve(any())).thenReturn("reservation-1")
        val createdOrder = OrderFromStore().apply { orderId = 42 }
        whenever(orderService.createOrder(any())).thenReturn(createdOrder)

        mockMvc.post("/cart/checkout") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CheckoutRequest("payment-token-123"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderId") { value(42) }
        }

        verify(stockReservationService).reserve(listOf(CartItemRequest("SKU1", 2)))
        verify(orderService).createOrder(
            OrderRequest(
                customerEmail = "user@example.com",
                paymentToken = "payment-token-123",
                cartItems = listOf(CartItemRequest("SKU1", 2)),
                reservationId = "reservation-1"
            )
        )
        verify(cartService).clear("user@example.com")
    }

    @Test
    fun `checkout rejects an empty cart with 400`() {
        whenever(cartService.getItems("user@example.com")).thenReturn(emptyMap())

        mockMvc.post("/cart/checkout") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CheckoutRequest("payment-token-123"))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `checkout surfaces insufficient stock as 409 without creating an order`() {
        whenever(cartService.getItems("user@example.com")).thenReturn(mapOf("SKU1" to 50))
        whenever(stockReservationService.reserve(any())).thenThrow(InsufficientStockException(listOf("SKU1")))

        mockMvc.post("/cart/checkout") {
            header("Authorization", authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CheckoutRequest("payment-token-123"))
        }.andExpect {
            status { isConflict() }
            jsonPath("$.insufficientSkuIds[0]") { value("SKU1") }
        }

        verify(orderService, org.mockito.kotlin.never()).createOrder(any())
    }
}
