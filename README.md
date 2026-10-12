# FlashSale — High-Concurrency Flash Sale Backend

A production-oriented Spring Boot backend designed to handle flash-sale purchase requests safely under high concurrency, preventing inventory overselling through atomic database operations.

## 🎯 Problem Statement

During flash sales, thousands of customers may attempt to purchase the same limited-stock product simultaneously. Without proper concurrency control, multiple requests can read the same stock value and oversell inventory.

**FlashSale addresses this race condition** by making PostgreSQL the source of truth for inventory and enforcing atomic stock updates.

## ✨ Key Features

* **Concurrency-safe inventory:** Atomic database updates prevent stock from dropping below zero.
* **Idempotent purchases:** Repeated requests with the same idempotency key reuse the existing order.
* **Reservation expiry:** Unpaid pending reservations expire automatically, and inventory is restored.
* **Scheduled processing:** Background scheduling checks for expired reservations.
* **Redis reservation support:** Includes Redis-based stock reservation components for further integration.
* **Automated testing:** Unit tests, PostgreSQL integration tests, and concurrent purchase tests.

## 🛠️ Tech Stack

* **Language:** Java
* **Framework:** Spring Boot
* **Database:** PostgreSQL
* **Caching / reservation support:** Redis
* **Data access:** Spring Data JPA / Hibernate
* **Build tool:** Maven
* **Testing:** JUnit, Spring Boot Test

## 🏗️ Core Purchase Flow

1. Receive a purchase request with a product ID and idempotency key.
2. Acquire a PostgreSQL transaction-level advisory lock for the idempotency key.
3. Return the existing order if the key has already been used.
4. Validate that the sale is active.
5. Decrease inventory using an atomic conditional database update.
6. Create a pending-payment order with a reservation expiry time.
7. Restore inventory when an eligible reservation expires.

## 🛡️ Concurrency and Consistency

PostgreSQL remains the authoritative inventory store. Stock is decremented only when inventory is available, preventing competing transactions from successfully reserving the same final unit.

Idempotency prevents duplicate orders for repeated requests using the same key. Reservation expiry updates an order only while it remains pending and eligible for expiry, helping ensure inventory is restored only once.

## 🧪 Testing

Run the complete test suite:

```bash
mvn test
```

The test suite includes concurrent purchase scenarios, duplicate idempotency keys, out-of-stock behavior, sale-window validation, and reservation expiry.

The concurrent load test has passed with the current test configuration. Record its actual request count, successful purchases, rejected purchases, execution time, and throughput before publishing numerical performance claims.

## ⚙️ Configuration

Configure the following environment variables before starting the application:

| Variable            | Purpose                        |
| ------------------- | ------------------------------ |
| `DATABASE_URL`      | PostgreSQL JDBC connection URL |
| `DATABASE_USERNAME` | PostgreSQL username            |
| `DATABASE_PASSWORD` | PostgreSQL password            |
| `REDIS_HOST`        | Redis hostname                 |
| `REDIS_PORT`        | Redis port                     |
| `REDIS_PASSWORD`    | Redis password                 |

For tests using the local PostgreSQL test database, configure `LOCAL_DB_PASSWORD` as required.

Never commit real credentials or secrets to version control.

## 🚀 Running Locally

1. Install Java and Maven.

2. Start PostgreSQL and create a database.

3. Configure the required environment variables.

4. Ensure Redis connection settings are configured for the application.

5. Run the tests:

   ```bash
   mvn test
   ```

6. Start the application:

   ```bash
   mvn spring-boot:run
   ```

## 📌 Engineering Goals

This project focuses on practical backend engineering challenges:

* Race conditions and concurrency control
* Database transaction boundaries
* Idempotent API operations
* Inventory reservation and recovery
* Automated testing of concurrent workloads
* Reliable order state transitions

## 🔮 Future Improvements

* Integrate Redis reservation logic with PostgreSQL using a clearly defined consistency strategy.
* Add API documentation and request/response examples.
* Add authentication and authorization if required by the deployment scope.
* Run larger, repeatable load tests and publish measured results.
* Deploy the application and document its operational limitations.

## 👨‍💻 Author

Built as a backend engineering project to explore concurrency control, transactional consistency, and reliable inventory reservation in Java and Spring Boot.
