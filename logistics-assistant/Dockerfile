# Build the boot jar inside the image so the host only needs Docker.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
# Tests run in CI, not here — an image build must not depend on a reachable Docker daemon
# for Testcontainers.
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run unprivileged.
RUN useradd --system --create-home --uid 1001 spring
USER spring

COPY --from=build --chown=spring:spring /workspace/target/logistics-assistant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
