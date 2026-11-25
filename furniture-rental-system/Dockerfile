# ---- Build stage: compile the app and package the jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Download dependencies first. This layer is reused until pom.xml changes.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# ---- Run stage: a small Java runtime with only the jar ----
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /data/uploads \
    && chown -R app:app /data

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    FURENTO_UPLOAD_DIR=/data/uploads

EXPOSE 8085
VOLUME /data/uploads

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8085/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
