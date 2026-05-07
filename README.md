# Expense Categorizer Backend

A Spring Boot backend service for the Finailytics expense tracking and categorization application.

## Overview

The Expense Categorizer backend provides RESTful APIs for managing users, transactions, budgets, and chat history. It integrates with a relational database (managed with Flyway migrations) and supports user authentication and security features.

## Technology Stack

- **Framework**: Spring Boot
- **Language**: Java
- **Build Tool**: Maven
- **Database**: Configured via Flyway migrations
- **Authentication**: Security configuration included


## Project Structure

```
src/
├── main/
│   ├── java/com/expense/expense_categorizer/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # Entity models
│   │   ├── parser/          # Parsers for data processing
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # Security configurations
│   │   └── service/         # Business logic layer
│   └── resources/
│       ├── application.yaml # Application configuration
│       ├── db/migration/    # Flyway migration scripts
│       │   ├── V1__create_users.sql
│       │   ├── V2__create_transactions.sql
│       │   ├── V3__create_budgets.sql
│       │   └── V4__create_chat_history.sql
│       ├── static/          # Static resources
│       └── templates/       # Template resources
└── test/                    # Test files
```

## Database Migrations

The database schema is managed through Flyway migrations:
- **V1**: Users table
- **V2**: Transactions table
- **V3**: Budgets table
- **V4**: Chat history table

## Building the Application

### Prerequisites
- Java 8+ (or version specified in `system.properties`)
- Maven 3.6+

### Build Command
```bash
./mvnw clean package
```

### Build on Windows
```bash
mvnw.cmd clean package
```

## Running the Application

### From IDE
Run the main Spring Boot application class from your IDE.

### From Command Line
```bash
./mvnw spring-boot:run
```

### From Built JAR
```bash
java -jar target/expense-categorizer-0.0.1-SNAPSHOT.jar
```

## Docker

A Dockerfile is included for containerized deployment:
```bash
docker build -t expense-categorizer .
docker run -p 8080:8080 expense-categorizer
```

## Configuration

Edit `src/main/resources/application.yaml` to configure:
- Server port
- Database connection settings
- Authentication parameters
- Other application-specific settings

## Testing

Run the test suite:
```bash
./mvnw test
```

## Key Components

### Controllers
REST endpoints for managing expenses, budgets, transactions, and chat functionality.

### Services
Business logic for expense categorization, budget management, and user operations.

### Repository Layer
Database access using Spring Data JPA or similar persistence frameworks.

### Security
Security configurations for user authentication and authorization.

### DTOs
Data Transfer Objects for API request/response handling.

## API Documentation

API endpoints are available at `/api/` (specific endpoints depend on controller implementations).

## Support

For issues or questions, please refer to the HELP.md file or contact the development
