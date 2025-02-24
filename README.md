# Instant Payment API

The **Instant Payment API** is a Spring Boot microservice that lets users send money instantly via REST. It supports transactional processing, concurrency handling, error management, and asynchronous notifications using Kafka. Transactions are stored in a PostgreSQL database, and the service is built for high availability and fault tolerance.

## Features

- **Instant Payments**: Transfers are processed instantly after checking the sender's balance.
- **Concurrency Handling**: Prevents double spending with pessimistic locking on account updates.
- **Atomic Transactions**: Ensures account balance updates and transaction records are processed together.
- **Asynchronous Notifications**: Uses Kafka to send real-time updates to recipients.
- **Error Handling & Fault Tolerance**: Global exception handling manages issues like insufficient funds or invalid accounts.
- **API Documentation**: Swagger documentation is available at `/swagger-ui/index.html`.

## Getting Started

### Prerequisites
Make sure you have the following installed:
- **Java** (JDK 17+)
- **Maven** (3.6+)
- **PostgreSQL** (or an in-memory database for testing)
- **Kafka** (for async notifications)

### Setup

1. Clone the repo and navigate into it.
2. Create the databases with the following names:
   - paymentdb
   - paymentdb-test
3. Set the postgres credentials in both property files.

**Sample accounts are added by liquibase**

Build the project with Maven:

```bash
mvn clean package
```

This compiles the code, runs tests, and packages the app into an executable JAR.

### Running the App

Start the app using the Spring Boot plugin:

```bash
mvn spring-boot:run
```

Or run the packaged JAR directly:

```bash
java -jar target/instant-payment-api-1.0.0.jar
```

## Docker Support

To run the app in docker execute the following:

```bash
docker-compose up --build
```

## API Documentation

Swagger UI is automatically generated. Once the app is running, open:

```bash
http://localhost:8080/swagger-ui/index.html
```

This lets you explore API endpoints, send test requests, and review request/response formats.

## Key API Endpoint

### **POST /api/payments**
Processes a payment request.

#### Request Body

```json
{
  "senderId": 1,
  "recipientId": 2,
  "amount": 200.00
}
```

#### Response
A JSON object with a transaction ID and confirmation message.

## Running Tests

The project includes unit tests with **JUnit 5** and **Mockito**. Tests are run automatically during the Maven build, but you can run them manually:

```bash
mvn test
```
A separate database is used for testing.