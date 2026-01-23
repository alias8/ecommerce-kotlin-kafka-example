package org.example.ecommerceexamplebackendkotlinkafka.order

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

data class CreateOrderRequest(
    val customerId: String,
    val productId: String,
    val quantity: Int,
    val totalAmount: BigDecimal
)

data class OrderResponse(
    val id: String,
    val customerId: String,
    val productId: String,
    val quantity: Int,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val createdAt: String
)

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderKafkaProducer: OrderKafkaProducer
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        logger.info("Creating order for customer: ${request.customerId}")

        val order = Order(
            customerId = request.customerId,
            productId = request.productId,
            quantity = request.quantity,
            totalAmount = request.totalAmount
        )

        val savedOrder = orderRepository.save(order)
        logger.info("Order saved to database with ID: ${savedOrder.id}")

        val event = OrderCreatedEvent(
            orderId = savedOrder.id!!,
            customerId = savedOrder.customerId,
            productId = savedOrder.productId,
            quantity = savedOrder.quantity,
            totalAmount = savedOrder.totalAmount,
            createdAt = savedOrder.createdAt
        )
        orderKafkaProducer.sendOrderCreatedEvent(event)

        return savedOrder.toResponse()
    }

    fun getOrder(orderId: String): OrderResponse? {
        return orderRepository.findById(orderId)
            .map { it.toResponse() }
            .orElse(null)
    }

    fun getOrdersByCustomer(customerId: String): List<OrderResponse> {
        return orderRepository.findByCustomerId(customerId)
            .map { it.toResponse() }
    }

    fun getAllOrders(): List<OrderResponse> {
        return orderRepository.findAll()
            .map { it.toResponse() }
    }

    private fun Order.toResponse() = OrderResponse(
        id = this.id!!,
        customerId = this.customerId,
        productId = this.productId,
        quantity = this.quantity,
        totalAmount = this.totalAmount,
        status = this.status,
        createdAt = this.createdAt.toString()
    )
}