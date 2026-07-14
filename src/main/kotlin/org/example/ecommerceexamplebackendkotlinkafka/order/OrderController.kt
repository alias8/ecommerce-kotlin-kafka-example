package org.example.ecommerceexamplebackendkotlinkafka.order

import jakarta.validation.Valid
import org.example.ecommerceexamplebackendkotlinkafka.product.ProductNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun createOrder(@Valid @RequestBody order: OrderRequest): ResponseEntity<OrderFromStore> {
        val createOrder = orderService.createOrder(order)
        return ResponseEntity.status(HttpStatus.CREATED).body(createOrder)
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: Long): ResponseEntity<OrderFromStore> {
        return ResponseEntity.ok(orderService.getOrder(orderId))
    }

    @PutMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @Valid @RequestBody request: OrderStatusUpdateRequest
    ): ResponseEntity<OrderFromStore> {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request))
    }

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(e: OrderNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            mapOf(
                "field" to fieldError.field,
                "message" to (fieldError.defaultMessage ?: "Invalid value")
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("errors" to errors)
        )
    }

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            e.message
        )
    }
}