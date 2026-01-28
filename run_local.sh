#!/bin/bash
# Start infrastructure services, then run the app locally

echo "Stopping any running containers..."
docker-compose down -v

echo "Starting infrastructure (Kafka, MongoDB, Redis, RabbitMQ)..."
docker-compose up -d kafka mongodb redis rabbitmq

echo "Waiting for services to be ready..."
sleep 5

echo "Starting Spring Boot app..."
./gradlew bootRun