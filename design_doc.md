# Design Doc

An example simple e-commerce backend where services communicate via Kafka events, not direct calls.

Services (can all be small)

1. Order Service (producer)
2. Payment Service
3. Inventory Service (consumer)
4. Email/Notification Service (consumer)
5. Analytics Service

Flow:

1. User places order and order with the REST endpoint `POST /order`, this order is then written to database
2. Emits Kafka event to Order Service
3. Multiple services consume the same event, no service calls another directly.
   order_created →
   Payment Service
   Inventory Service
   Email Service
   Analytics Service
4. Each service emits its own events:

- payment_succeeded
- inventory_reserved
- order_failed
- email_sent
- Kafka becomes the system’s backbone.

# App structure:

Each service:

* Its own project
* Its own database
* Its own Kafka consumer group

e-commerce-example/
│
├─ docker-compose.yml
│
├─ order_service/
│ └─ orders/
│
├─ payment_service/
│ └─ payments/
│
├─ inventory_service/
│ └─ inventory/
│
├─ notification_service/
│ └─ notifications/
