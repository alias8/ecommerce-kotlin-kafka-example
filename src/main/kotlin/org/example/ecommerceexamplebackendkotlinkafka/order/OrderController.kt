package org.example.ecommerceexamplebackendkotlinkafka.order

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun createOrder(@RequestBody order: OrderRequest): ResponseEntity<OrderFromStore> {
        val createOrder = orderService.createOrder(order)
        return ResponseEntity.status(HttpStatus.CREATED).body(createOrder)
    }

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            e.message
        )
    }
}