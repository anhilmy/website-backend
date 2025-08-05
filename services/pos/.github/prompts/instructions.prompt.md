---
mode: ask
---

# ChatGPT Instructions for Point of Sales System Development

## 1. Project Setup Instructions

### 1.1 Initial Project Structure

```
Follow these steps to create the project structure:

1. Create a new Maven project with the following structure:
   pos-system/
   ├── src/
   │   ├── main/
   │   │   ├── java/
   │   │   │   └── com/
   │   │   │       └── pos/
   │   │   │           ├── Main.java
   │   │   │           ├── config/
   │   │   │           ├── controllers/
   │   │   │           ├── models/
   │   │   │           ├── services/
   │   │   │           ├── repositories/
   │   │   │           ├── middleware/
   │   │   │           ├── utils/
   │   │   │           └── exceptions/
   │   │   └── resources/
   │   │       ├── application.properties
   │   │       ├── logback.xml
   │   │       └── db/
   │   │           └── migration/
   │   └── test/
   │       └── java/
   │           └── com/
   │               └── pos/
   ├── pom.xml
   ├── Dockerfile
   ├── docker-compose.yml
   ├── README.md
   └── openapi.yaml

2. Use Java 21 and Maven as specified in requirements
3. Include all necessary dependencies in pom.xml
```

### 1.2 Maven Dependencies

```
Include these essential dependencies in pom.xml:

<!-- SparkJava Framework -->
<dependency>
    <groupId>com.sparkjava</groupId>
    <artifactId>spark-core</artifactId>
    <version>2.9.4</version>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.42.0.0</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
</dependency>

<!-- ORM -->
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.2.6.Final</version>
</dependency>

<!-- JWT Authentication -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.8</version>
</dependency>

<!-- Kafka (for logging) -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.5.0</version>
</dependency>

<!-- Password Hashing -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

## 2. Development Phase Instructions

### 2.1 Phase 1: Core Infrastructure

```
Complete these tasks in order:

1. Create Main.java with SparkJava server setup
2. Implement database configuration classes for SQLite and PostgreSQL
3. Create all data models (User, MenuCategory, MenuItem, Transaction, TransactionItem)
4. Set up Hibernate configuration and entity mappings
5. Create database initialization and migration scripts
6. Implement basic logging configuration
7. Create configuration management system
```

### 2.2 Phase 2: Authentication System

```
Implement authentication in this sequence:

1. Create User model with password hashing
2. Implement JWT token generation and validation
3. Create authentication middleware
4. Build login endpoint with session management
5. Add password security features (BCrypt)
6. Implement session timeout and token refresh
7. Add rate limiting for login attempts
```

### 2.3 Phase 3: Menu Management

```
Build menu system with these steps:

1. Create MenuCategory repository and service
2. Implement MenuCategory CRUD operations
3. Create MenuItem repository and service
4. Implement MenuItem CRUD operations with category relationships
5. Build public menu listing endpoints (no authentication required)
6. Add menu search and filtering capabilities
7. Implement item availability management
```

### 2.4 Phase 4: Order Management

```
Develop order system following this approach:

1. Create Transaction and TransactionItem models
2. Implement order creation with item validation
3. Build order update functionality (add/remove items)
4. Create automatic price calculation with tax
5. Implement order state management (draft, cooking, completed)
6. Add order completion with payment recording
7. Ensure completed orders are immutable
8. Create order lookup and history features
```

### 2.5 Phase 5: Advanced Features

```
Implement advanced features:

1. Set up comprehensive request/response logging
2. Create multiple logging outputs (file, Kafka, console)
3. Implement structured JSON logging
4. Add performance metrics and monitoring
5. Create health check endpoints
6. Implement input validation and security measures
7. Add error handling and custom exceptions
```

### 2.6 Phase 6: API Documentation

```
Create OpenAPI documentation:

1. Write complete openapi.yaml specification
2. Include all endpoints with request/response schemas
3. Add authentication requirements to protected endpoints
4. Include example requests and responses
5. Set up Swagger UI integration
6. Document error responses and status codes
```

### 2.7 Phase 7: Testing

```
Implement comprehensive testing:

1. Create unit tests for all models and services
2. Build integration tests for API endpoints
3. Test authentication and authorization flows
4. Verify database operations with test data
5. Test logging functionality across all outputs
6. Create performance and load tests
7. Add end-to-end scenario tests
```

## 3. Code Quality Guidelines

### 3.1 Coding Standards

```
Follow these coding practices:

1. Use proper Java naming conventions
2. Implement proper exception handling with custom exceptions
3. Add comprehensive JavaDoc comments
4. Follow SOLID principles in design
5. Use dependency injection where appropriate
6. Implement proper resource management (try-with-resources)
7. Follow RESTful API design principles
```

### 3.2 Security Implementation

```
Implement security measures:

1. Validate all input parameters
2. Use parameterized queries to prevent SQL injection
3. Implement proper error handling without information leakage
4. Add CORS configuration for cross-origin requests
5. Implement rate limiting on critical endpoints
6. Use HTTPS in production configuration
7. Add security headers to responses
```

### 3.3 Performance Optimization

```
Optimize for performance:

1. Implement database connection pooling
2. Use proper indexing on database tables
3. Implement caching for frequently accessed data
4. Optimize database queries
5. Use async processing where beneficial
6. Implement proper resource cleanup
7. Monitor and log performance metrics
```

## 4. Configuration Instructions

### 4.1 Environment Configuration

```
Set up configuration management:

1. Create application.properties for each environment
2. Use environment variables for sensitive data
3. Implement configuration classes with validation
4. Support runtime configuration updates
5. Create development, staging, and production profiles
6. Document all configuration options
```

### 4.2 Database Configuration

```
Configure database support:

1. Create abstract database configuration
2. Implement SQLite configuration for development
3. Implement PostgreSQL configuration for production
4. Add connection pooling configuration
5. Create database migration system
6. Implement database health checks
```

### 4.3 Logging Configuration

```
Set up comprehensive logging:

1. Configure structured JSON logging
2. Implement file-based logging with rotation
3. Set up Kafka producer for log streaming
4. Configure console logging for development
5. Add contextual logging with request IDs
6. Implement different log levels for different environments
```

## 5. Testing Instructions

### 5.1 Unit Testing Strategy

```
Create comprehensive unit tests:

1. Test all model classes and their validations
2. Test all service classes with mocked dependencies
3. Test utility classes and helper functions
4. Test configuration classes
5. Achieve minimum 80% code coverage
6. Use meaningful test names and descriptions
```

### 5.2 Integration Testing

```
Build integration tests:

1. Test database operations with real database
2. Test API endpoints with full request/response cycle
3. Test authentication flows end-to-end
4. Test logging outputs to all channels
5. Test error scenarios and edge cases
6. Test concurrent access scenarios
```

## 6. Documentation Requirements

### 6.1 Code Documentation

```
Document the codebase:

1. Add JavaDoc to all public classes and methods
2. Include usage examples in documentation
3. Document configuration options
4. Create architecture documentation
5. Document API endpoints and their purposes
6. Include troubleshooting guides
```

### 6.2 Deployment Documentation

```
Create deployment guides:

1. Document local development setup
2. Create Docker deployment instructions
3. Document production deployment steps
4. Include database setup and migration instructions
5. Document monitoring and logging setup
6. Create backup and recovery procedures
```

## 7. Quality Assurance Instructions

### 7.1 Code Review Checklist

```
Before considering code complete, verify:

1. All features from the feature list are implemented
2. All tests pass and coverage meets requirements
3. Security measures are properly implemented
4. Error handling is comprehensive
5. Logging is working across all channels
6. Documentation is complete and accurate
7. Performance requirements are met
```

### 7.2 Final Validation

```
Complete system validation:

1. Test all user scenarios end-to-end
2. Verify database switching between SQLite and PostgreSQL
3. Test logging outputs to file, Kafka, and console
4. Validate OpenAPI specification accuracy
5. Test deployment with Docker
6. Verify security measures are effective
7. Confirm all requirements are met
```
