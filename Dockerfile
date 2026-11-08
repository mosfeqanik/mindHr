# Root-level Dockerfile so Render can build the API with the default settings
# (Root Directory empty, Dockerfile path ./Dockerfile). The code stays in backend/;
# this file only copies it from there. backend/Dockerfile is still used by
# render.yaml (rootDir: backend) and by `docker compose` in backend/.

# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline

COPY backend/src ./src
RUN mvn clean package -DskipTests


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Keep the heap inside the container memory limit (Render free plan has 512 MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# sh -c so $JAVA_OPTS is expanded; exec keeps java as PID 1 so it gets stop signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
