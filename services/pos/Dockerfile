# Point of Sales System Dockerfile
# Multi-stage build for optimal image size

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add metadata
LABEL maintainer="POS System Team"
LABEL version="1.0.0"
LABEL description="Point of Sales System Backend Application"

# Create non-root user for security
RUN addgroup -g 1000 posuser && \
    adduser -D -s /bin/sh -u 1000 -G posuser posuser

# Install necessary packages
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init

# Set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set working directory
WORKDIR /app

# Copy built fat JAR from builder stage
COPY --from=builder /app/target/pos-app.jar app.jar

# Create directories for logs and data
RUN mkdir -p /app/logs /app/data && \
    chown -R posuser:posuser /app

# Switch to non-root user
USER posuser

# Expose application port
EXPOSE 4567

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:4567/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# Application environment variables
ENV SERVER_PORT=4567
ENV LOG_LEVEL=INFO
ENV PROFILE=docker

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]