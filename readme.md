# E-Commerce Example Backend (Kotlin + Kafka)

An example application demonstrating event-driven architecture for e-commerce using Kotlin, Spring Boot, and Apache Kafka.

# To run:
Only runs locally at the moment, not in docker.
`run_local.sh`

## Overview

This app simulates an e-commerce backend where services communicate asynchronously via Kafka events rather than direct REST calls. This pattern is used by large-scale systems (Amazon, Uber, Netflix) to handle high volumes and allow services to scale independently.

## Architecture

```
Customer places order
        |
   OrderService
   (saves order, emits OrderCreatedEvent)
        |
   PaymentService
   (processes payment, emits PaymentCreatedEvent)
        |
   ProductService
   (updates inventory, emits ProductUpdatedEvent)
        |
   ShippingService
   (creates shipment OR emits LowStockEvent)
```

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| `orders` | New order events |
| `payments` | Payment processing results |
| `products` | Inventory updates |
| `warehouse-alerts` | Notify warehouse to pick/pack items |
| `inventory-alerts` | Low stock alerts for restocking |

## Tech Stack

- Kotlin
- Spring Boot
- Spring Kafka
- Spring Data JPA
- H2 Database (in-memory)

## Running the App

```bash
./run_docker_start.sh
```

## API

### Create Order

```bash
POST /orders
Content-Type: application/json

{
  "customerEmail": "customer@example.com",
  "paymentToken": "tok_123",
  "cartItems": [
    { "skuId": "SKU001", "quantity": 2 }
  ]
}
```

## Purpose

This is a learning/example application to demonstrate:
- Event-driven microservice patterns
- Kafka producers and consumers in Spring Boot
- Asynchronous service communication
- Handling partial fulfillment (some items ship, others backordered)