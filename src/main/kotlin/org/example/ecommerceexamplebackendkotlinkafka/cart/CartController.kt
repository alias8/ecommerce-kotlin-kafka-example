package org.example.ecommerceexamplebackendkotlinkafka.cart

import jakarta.validation.Valid
import org.example.ecommerceexamplebackendkotlinkafka.inventory.InsufficientStockException
import org.example.ecommerceexamplebackendkotlinkafka.inventory.StockReservationService
import org.example.ecommerceexamplebackendkotlinkafka.order.CartItemRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderFromStore
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderRequest
import org.example.ecommerceexamplebackendkotlinkafka.order.OrderService
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/cart")
class CartController(
    private val cartService: CartService,
    private val stockReservationService: StockReservationService,
    private val orderService: OrderService
) {

    @GetMapping
    fun getCart(@AuthenticationPrincipal email: String): ResponseEntity<CartResponse> =
        ResponseEntity.ok(cartService.viewCart(email))

    @PostMapping("/items")
    fun addItem(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: AddCartItemRequest
    ): ResponseEntity<CartResponse> {
        cartService.addItem(email, request.skuId!!, request.quantity)
        return ResponseEntity.ok(cartService.viewCart(email))
    }

    @PutMapping("/items/{skuId}")
    fun updateItem(
        @AuthenticationPrincipal email: String,
        @PathVariable skuId: String,
        @Valid @RequestBody request: UpdateCartItemRequest
    ): ResponseEntity<CartResponse> {
        cartService.setItemQuantity(email, skuId, request.quantity)
        return ResponseEntity.ok(cartService.viewCart(email))
    }

    @DeleteMapping("/items/{skuId}")
    fun removeItem(
        @AuthenticationPrincipal email: String,
        @PathVariable skuId: String
    ): ResponseEntity<CartResponse> {
        cartService.removeItem(email, skuId)
        return ResponseEntity.ok(cartService.viewCart(email))
    }

    @PostMapping("/checkout")
    fun checkout(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: CheckoutRequest
    ): ResponseEntity<OrderFromStore> {
        val cartItems = cartService.getItems(email).map { (skuId, quantity) -> CartItemRequest(skuId, quantity) }
        if (cartItems.isEmpty()) {
            throw EmptyCartException("Cart is empty")
        }

        val reservationId = stockReservationService.reserve(cartItems)

        val order = orderService.createOrder(
            OrderRequest(
                customerEmail = email,
                paymentToken = request.paymentToken,
                cartItems = cartItems,
                reservationId = reservationId
            )
        )
        cartService.clear(email)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(e: InsufficientStockException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            mapOf("insufficientSkuIds" to e.skuIds, "message" to (e.message ?: "Insufficient stock"))
        )

    @ExceptionHandler(EmptyCartException::class)
    fun handleEmptyCart(e: EmptyCartException): ResponseEntity<String> =
        ResponseEntity.badRequest().body(e.message)

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            mapOf(
                "field" to fieldError.field,
                "message" to (fieldError.defaultMessage ?: "Invalid value")
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("errors" to errors))
    }
}
