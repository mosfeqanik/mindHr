# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot application
RUN mvn clean package -DskipTests


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated JAR
COPY --from=build /app/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Start application
# Keep the heap inside the container memory limit (Render free plan has 512 MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# sh -c so $JAVA_OPTS is expanded; exec keeps java as PID 1 so it gets stop signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]