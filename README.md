# BookStore Microservices

A Spring Boot microservices-based bookstore application built with Eureka, Spring Cloud Gateway, OpenFeign, JWT-based security, and H2 in-memory persistence.

This project was started in May 2025 and completed in June 2025.

## Overview

BookStore Microservices is a modular backend system for managing users, books, and orders across multiple independent services. The application demonstrates service discovery, API gateway routing, JWT-based authentication, and inter-service communication in a distributed Spring Boot architecture.

## Architecture

The system is composed of the following services:

- Eureka Server: service registration and discovery
- API Gateway: single entry point and JWT validation
- Auth Service: registration, login, and JWT issuance
- Book Service: catalog and stock management
- Order Service: order creation and client-side communication with Book Service

## Service Ports

- Eureka Server: http://localhost:8761
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Book Service: http://localhost:8082
- Order Service: http://localhost:8083

## Tech Stack

- Java 17
- Spring Boot 3.2.x
- Spring Cloud 2023.0.x
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- OpenFeign
- Spring Security
- JWT (jjwt)
- Spring Data JPA
- H2 Database
- Maven
- Docker

## Project Structure

```text
bookstore-microservices/
+-- api-gateway/
+-- auth-service/
+-- book-service/
+-- eureka-server/
+-- order-service/
+-- docker-compose.yml
+-- .env.example
+-- run-all.ps1
+-- README.md
+-- .gitignore
```

## Prerequisites

Before running the project locally, make sure you have:

- Java 17 or above installed
- Maven installed
- Docker Desktop installed if using Docker Compose
- Git installed

## Run Locally with Maven

From the project root, run the services in this order:

```bash
cd eureka-server
mvn spring-boot:run
```

Open another terminal:

```bash
cd auth-service
mvn spring-boot:run
```

```bash
cd book-service
mvn spring-boot:run
```

```bash
cd order-service
mvn spring-boot:run
```

```bash
cd api-gateway
mvn spring-boot:run
```

Check Eureka Dashboard:

```text
http://localhost:8761
```

## Run with Docker Compose

If Docker is installed and running, start the full stack with:

```bash
docker compose up --build -d
```

Check running containers:

```bash
docker compose ps
```

Stop everything:

```bash
docker compose down
```

## Environment Configuration

A sample environment file is available at `.env.example`.

```bash
cp .env.example .env
```

Update the values as needed for your local environment.

## API Flow

All client requests are routed through the API Gateway on port 8080.

Typical flow:

1. Client calls the API Gateway
2. Gateway validates JWT
3. Gateway forwards the request to the correct service through Eureka discovery
4. Service processes the request and returns the response

## Example Requests

### Register user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sahil",
    "email": "sahil@example.com",
    "password": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sahil@example.com",
    "password": "password123"
  }'
```

### Get books

```bash
curl http://localhost:8080/api/books
```

### Create order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"bookId": 1, "quantity": 2}
    ]
  }'
```

## Notes

- H2 is used for local development and resets on restart.
- JWT secrets should be moved to environment variables or a secret manager in production.
- This project is intended as a microservices learning and demo implementation.

## Suggested Industry-Standard Commit Messages

For a professional project history, the commit messages should look like this:

```text
feat: initialize BookStore microservices platform
feat: add API gateway with JWT validation
feat: implement auth service with JWT authentication
feat: add book catalog and stock management
feat: implement order management and inter-service communication
docs: add project README and setup documentation
```
