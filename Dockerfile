# ===============================
# Multi-stage build for production
# ===============================

# -------------------------
# Build stage
# -------------------------
FROM maven:3.8.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Add build timestamp to force cache invalidation
RUN echo "Build timestamp: $(date)" > build-info.txt

# Build the application
RUN mvn clean package -DskipTests

# -------------------------
# Production stage
# -------------------------
FROM eclipse-temurin:17-jre-alpine

# Install necessary packages
RUN apk add --no-cache curl bash

# Create app user
RUN addgroup -g 1001 appuser && adduser -D -u 1001 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
