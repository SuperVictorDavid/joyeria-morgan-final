# Joyeria Morgan - inventario-service
# Build multi-stage: compila el jar dentro de la imagen, no necesita build/ en la maquina.
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN sh ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]