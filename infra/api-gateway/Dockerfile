# API Gateway (Maven: ./mvnw package)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8762
ENTRYPOINT ["java","-jar","app.jar"]
