# Config Server (Maven) - native: el config-repo viaja dentro de la imagen.
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/config-repo ./config-repo
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
