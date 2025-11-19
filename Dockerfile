# Multi-stage Docker build for Luck & Reward Engine (Java 8 Version)
# This Dockerfile creates an optimized container with Java 8

# Stage 1: Build the application using Java 8
FROM maven:3.6-jdk-8 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image with Java 8
FROM openjdk:8-jre-alpine

# Add metadata
LABEL maintainer="gaming-team"
LABEL description="Luck and Reward Engine - Real-Time Gaming Service (Java 8)"
LABEL version="1.0.0"

# Create app directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/luck-reward-engine-*.jar app.jar

# Create a non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port 8080
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
