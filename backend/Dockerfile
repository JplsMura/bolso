# Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Testes rodam fora do Docker (./mvnw verify), porque os de integração precisam do Docker.
RUN mvn -B -q package -DskipTests

# Execução
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 bolso
COPY --from=build /app/target/bolso-api-*.jar app.jar
USER bolso
ENV TZ=UTC
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
