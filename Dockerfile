# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the fat jar
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as an unprivileged user (let the system assign the UID; the base image
# already occupies UID 1000)
RUN useradd --system --no-create-home clouddnsync \
    && mkdir -p /app/config /app/logs \
    && chown -R clouddnsync:clouddnsync /app

COPY --from=build /build/target/clouddnsync-1.0.0-jar-with-dependencies.jar /app/clouddnsync.jar

USER clouddnsync

# Config is provided via a mounted volume at /app/config/config.yml.
# Override with the CONFIG_PATH env var if needed.
ENV CONFIG_PATH=/app/config/config.yml

VOLUME ["/app/config", "/app/logs"]

ENTRYPOINT ["java", "-jar", "/app/clouddnsync.jar"]
