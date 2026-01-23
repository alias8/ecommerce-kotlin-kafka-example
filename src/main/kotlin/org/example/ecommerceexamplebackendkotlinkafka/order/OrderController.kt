package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: String): ResponseEntity<OrderResponse> {
        val order = orderService.getOrder(orderId)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderResponse>> {
        return ResponseEntity.ok(orderService.getAllOrders())
    }

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: String): ResponseEntity<List<OrderResponse>> {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId))
    }
}