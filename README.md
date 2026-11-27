# FURENTO

[![CI](https://github.com/IshanShabin/FURENTO/actions/workflows/ci.yml/badge.svg)](https://github.com/IshanShabin/FURENTO/actions/workflows/ci.yml)

FURENTO is a full-stack furniture rental platform built as a portfolio project. Customers browse furniture, build a rental order, pay online or on delivery, and track it through to return. Administrators manage the catalog and move rentals through their lifecycle.

## Features

### Customers

- Browse furniture by category, with pricing, stock and reviews.
- Choose quantity and rental duration (1, 3, 6 or 12 months) and add items to a cart.
- Check out with a delivery address and pay online (UPI or card through Razorpay) or choose cash on delivery.
- Follow orders from a dashboard: status, due date and an overdue badge.
- View and print a tax invoice for every order.

### Administrators

- Create, edit and delete categories.
- Add, edit and delete furniture, with JPG or PNG image uploads (up to 5 MB each).
- Review incoming rentals: accept, cancel, or mark as returned.
- See an operations dashboard with pending and active rentals, available items and low stock.

## Rental lifecycle

| Status      | What it means                                | Can move to           |
| ----------- | -------------------------------------------- | --------------------- |
| `PENDING`   | Order placed, waiting for the admin          | `ACTIVE`, `CANCELLED` |
| `ACTIVE`    | Delivered. Start and end dates are set       | `RETURNED`            |
| `RETURNED`  | Items collected and put back in stock        | none                  |
| `CANCELLED` | Cancelled before delivery. Stock is restored | none                  |

- Accepting an order sets each item's start and end date, marks the delivery as delivered, and marks a cash-on-delivery payment as paid.
- An order shows **OVERDUE** when it is `ACTIVE` and past its latest end date. This is calculated, not stored.
- Returns are per order. An order that mixes rental durations is due on its latest end date, so the overdue badge and late fees start after that date.
- **Late fees:** when an overdue order is marked returned, the app records a late fee of one day's rent (monthly price / 30) per unit for each day late. It appears on the customer dashboard and the invoice until the admin marks it paid. Fees are settled outside the app and are not charged through Razorpay.
- The allowed moves live in one place (`RentalService`), so accept, cancel and return cannot skip a step.

## Technology

- Java 17 and Spring Boot 3.2.1
- Spring MVC and Thymeleaf (server-rendered pages), Bootstrap 5
- Spring Data JPA and Hibernate, MySQL 8
- Spring Security with role-based access (`ADMIN`, `CUSTOMER`), JWT in an HTTP-only cookie, and CSRF protection
- Razorpay for online payments, with server-side amount checks and signature verification
- JUnit 5, Mockito and Spring MVC Test
- Maven, GitHub Actions

## Getting started

### Prerequisites

- JDK 17
- Maven 3.9 or newer
- MySQL 8 running locally

### 1. Create the database

The app expects a database named `springdb` (override with `DB_URL`) and connects as `root` (override with `DB_USERNAME`). Tables are created automatically on first start.

```sql
CREATE DATABASE springdb;
```

### 2. Set environment variables

Secrets are read from the environment and are never committed.

| Variable                 | Required | Purpose                                                                                   |
| ------------------------ | -------- | ----------------------------------------------------------------------------------------- |
| `DB_PASSWORD`            | yes      | Database password                                                                         |
| `JWT_SECRET`             | yes      | Signing key for login tokens. At least 32 characters. The app refuses to start without it |
| `DB_URL`                 | no       | JDBC URL (default `jdbc:mysql://localhost:3306/springdb`)                                 |
| `DB_USERNAME`            | no       | Database user (default `root`)                                                            |
| `PORT`                   | no       | HTTP port (default `8085`)                                                                |
| `SPRING_PROFILES_ACTIVE` | no       | `dev` (default) or `prod`, see below                                                      |
| `ADMIN_USERNAME`         | no       | Username for the admin account created on startup                                         |
| `ADMIN_PASSWORD`         | no       | Admin password, at least 8 characters. With no admin variables, no admin is created       |
| `ADMIN_EMAIL`            | no       | Admin email (default `admin@furento.com`)                                                 |
| `RAZORPAY_KEY_ID`        | no       | Razorpay key id. Without Razorpay keys only cash on delivery is offered                   |
| `RAZORPAY_KEY_SECRET`    | no       | Razorpay key secret                                                                       |
| `JWT_COOKIE_SECURE`      | no       | Mark the login cookie `Secure`. Default `false` in dev and `true` in prod                 |
| `DDL_AUTO`               | no       | Prod only: Hibernate schema mode (default `update`)                                       |
| `FURENTO_UPLOAD_DIR`     | no       | Folder for uploaded product images (default `uploads/furniture`)                          |

### 3. Run

PowerShell:

```powershell
cd furniture-rental-system
$env:DB_PASSWORD    = "your-mysql-password"
$env:JWT_SECRET     = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "choose-a-strong-password"
mvn spring-boot:run
```

Bash:

```bash
cd furniture-rental-system
export DB_PASSWORD='your-mysql-password'
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='choose-a-strong-password'
mvn spring-boot:run
```

On Windows you can instead copy `furniture-rental-system/run-local.example.ps1` to `run-local.ps1` (which is git-ignored), fill in your values, and start the app with `.\run-local.ps1`. The script keeps the same `JWT_SECRET` between runs for you.

Open <http://localhost:8085>. On first start the app seeds the categories and a sample furniture catalog. Reuse the same `JWT_SECRET` between runs, otherwise existing logins stop working.

Registered users are always customers. The admin account comes only from the `ADMIN_*` variables.

## Run with Docker

This starts the app and a MySQL database together, with no Java or MySQL installed on your machine.

1. Install Docker Desktop.
2. Copy `.env.example` to `.env` and fill in the values (at least `DB_PASSWORD` and `JWT_SECRET`).
3. From the repo root, run:
   ```bash
   docker compose up --build
   ```
4. Open <http://localhost:8085>. The first start creates the tables and the sample catalog.

Useful commands:

| Command                      | What it does                                          |
| ---------------------------- | ----------------------------------------------------- |
| `docker compose logs -f app` | Follow the app logs                                   |
| `docker compose ps`          | Show whether both containers are healthy              |
| `docker compose down`        | Stop everything and keep the data                     |
| `docker compose down -v`     | Stop and also delete the database and uploaded images |

The compose file runs the `prod` profile with `JWT_COOKIE_SECURE=false` so login works over plain http. Set it to `true` in `.env` when you serve the site over HTTPS. Stop any copy of the app already running on port 8085 first.

## Profiles and health check

- **`dev`** (default): SQL logging on, template cache off, verbose app logs.
- **`prod`** (`SPRING_PROFILES_ACTIVE=prod`): no SQL logging, template cache on, login cookie marked `Secure`, proxy headers honoured, and error pages that never show internals. Over plain http, set `JWT_COOKIE_SECURE=false` or login will not stick.
- `GET /actuator/health` reports whether the app and its database are up. It needs no login and is meant for Docker and hosting health checks.

## Razorpay test mode

Online payments use Razorpay. Generate test keys in the Razorpay dashboard (see their docs), then set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`. Without keys, only cash on delivery is offered.

For test payments, use UPI `success@razorpay` to simulate success or `failure@razorpay` to simulate a failure.

## Tests and CI

```bash
cd furniture-rental-system
mvn clean verify
```

The tests use mocks and need neither MySQL nor any environment variables. They cover the cart, pricing and tax, the rental lifecycle and overdue logic, JWT handling, and API security.

GitHub Actions runs the same build on every push and pull request (`.github/workflows/ci.yml`).

## Project structure

```
furniture-rental-system/
  pom.xml
  src/main/java/com/furniture/rental/
    config/       security, upload and model configuration
    controller/   web and API endpoints
    dto/          cart and cart item
    entity/       JPA entities (rental, furniture, payment, delivery, ...)
    loader/       data and admin seeders
    repository/   Spring Data repositories
    security/     JWT service, filter and password encoder
    service/      rental, furniture, payment and user logic
    util/         pricing and tax rules
  src/main/resources/
    templates/    Thymeleaf pages (customer, admin, invoice)
    static/css/   styles
    furniture-catalog.json   sample catalog
  src/test/java/  unit and web-layer tests
```

## Known Limitations

- **Spring Boot version:** This project runs on Spring Boot 3.2.1, which reached end-of-life in December 2024 and no longer receives open-source security patches. It is functionally complete and secure for a portfolio demo. Upgrading to Spring Boot 4.1.x (Spring Framework 7, Jackson 3) is planned as a future enhancement and requires a staged migration due to breaking changes in the test layer (`@MockBean` → `@MockitoBean`) and the JWT library's Jackson dependency.
- **Payments:** Razorpay is in test mode only. No real money is processed. Refunds are not automated.
- **Late fees:** Not payable online; settled manually outside the app.
- **Image storage:** Furniture images are stored on the EC2 instance disk using a persistent Docker volume. Amazon S3 integration is a planned future improvement.
- **Rate-limiting counters:** Stored in memory; reset on application restart and do not work across multiple servers.

## Roadmap

- Damage penalties recorded when an order is returned
- Automatic Razorpay refunds for cancelled paid orders
- GST-compliant invoices with per-item rates

## License

Portfolio project. All rights reserved.
