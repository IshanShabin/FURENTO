# FURENTO

FURENTO is a full-stack furniture rental platform built as a portfolio project. It gives customers a place to explore furniture, build rental orders, and track them, while providing administrators with tools to manage the catalog and rental operations.

## What It Does

### For Customers

- Browse furniture by category and check rental pricing and availability.
- Choose item quantities and rental durations.
- Review a cart, provide delivery details, and place an order.
- View rental orders from a customer dashboard.

### For Administrators

- Create and manage furniture categories.
- Add, update, and remove furniture listings.
- Review rental orders and manage their status.

## Technology

- Java 17 and Spring Boot 3
- Spring MVC and Thymeleaf for server-rendered pages
- Spring Data JPA and Hibernate
- Spring Security for authentication and role-based access
- MySQL for persistent application data
- Maven for dependency management and builds

## Application Structure

The application follows a layered Spring architecture:

- `controller` handles web requests and page navigation.
- `service` contains rental, catalog, and user operations.
- `repository` provides database access through Spring Data JPA.
- `entity` defines the persistent domain model.
- `templates` contains the Thymeleaf customer and admin pages.

## Engineering Highlights

- Models rentals as orders with associated items, delivery details, and payment records.
- Calculates rental totals from item price, quantity, and rental duration.
- Updates furniture stock as rentals are created.
- Separates customer and administrator workflows with Spring Security roles.

This project demonstrates a complete server-rendered web application workflow, from catalog browsing through rental management.
