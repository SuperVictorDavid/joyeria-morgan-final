# Joyería Morgan - Proyecto Final DAW II (4697)

Negocio rescatado de `JoyeriaMorgaMVC` (ASP.NET MVC): inventario de joyas + ventas con carrito/boleta.
Técnica 100% del repo del curso `DESARROLLO-DE-APLICACIONES-WEB-II`: microservicios Spring Boot 3.2.5 Java 17 + JWT + Feign + RabbitMQ/Kafka + Eureka + Gateway + Angular.

## Estructura
```
backend/auth-service:8083      login JWT BCrypt, users (solo auth)
backend/clientes-service:8084  clientes CRUD GET/POST/PUT/DELETE
backend/inventario-service:8081 joyas CRUD + Rabbit/Kafka descuenta stock
backend/ventas-service:8082    ventas CRUD + Feign lb://inventario + CB/Retry + Kafka/Rabbit
backend/reportes-service:8085  agregador GET /reports/resumen|inventario|ventas (Feign a los 3)
infra/eureka-server:8761       descubrimiento (T1)
infra/config-server:8888       configuración centralizada (native config-repo/)
infra/api-gateway:8762         rutas lb://auth,clientes,inventario,ventas,reportes + CORS 4200
frontend/joyeria-front:4200    Angular 22 login + guard + interceptor + dashboard + productos + clientes + historial + carrito + reportes PDF
database/                      docker-compose MySQL 5510 joyeria_db + script.sql
docker-compose.full.yml        todo lo anterior en un compose (U2 Docker)
postman/                       Joyeria-Flows (5 carpetas: auth, inventario, ventas, clientes, reportes+CB+actuator)
```

## U2 Docker/K8s
- Docker por servicio (`backend/*/Dockerfile`, `infra/*/Dockerfile`): `eclipse-temurin:17-jre`, `COPY build/libs/*.jar` (Gradle `bootJar`) o `target/*.jar` (Maven `package`)
- Full: `docker compose -f docker-compose.full.yml up -d --build` (mysql 5510 + eureka 8761 + config 8888 + 5 backends + gateway 8762)
- K8s: `k8s/*-deployment.yaml` + `*-service.yaml` (NodePort 30081-30085, 30061-30062, 30088, 30306, patrón `resiliencia-demo/k8s`). Aplicar: `kubectl apply -f k8s/`

## Mapeo Examen T1 -> Proyecto (por eso va incluido)
- Feign Client: `ventas-service/client/ProductClient.java` name=`inventario-service` (por Eureka) + `GET /sales/{id}/details` y `GET /sales/{id}/details-resiliente` (CB)
- RabbitMQ: `stock-reserve-queue`/`stock-exchange` en ambos servicios, `POST /sales/rabbit-reserve`, `StockReserveConsumer.decreaseStock()`, alerta `stock.low` si <5
- Kafka: topic `stock-movements`, `POST /sales` publica `StockMovementEvent`, `StockUpdateConsumer` descuenta
- Spring Cloud: Eureka 8761 + Gateway 8762 + `spring-cloud-starter-netflix-eureka-client` en los 3 backends + `springCloud 2023.0.1`
- Seguridad (Unidad 2): JWT `auth-service/security/*`, BCrypt, `POST /auth/login`
- Resiliencia/Docker/K8s (clase): `resiliencia-demo`, `spring_cloud`, `database/docker-compose.yml` como referencia

## Requisitos rúbrica
- Login REST con BCrypt: `POST /auth/login {username,password} -> {token}` `auth-service/security/*`
- Angular consume GET,POST,PUT,DELETE y persiste: `services/joya.ts`, `services/venta.ts`, `components/inventario`, `components/ventas`
- AP1: login sin errores + pruebas DAO insertar/actualizar/eliminar/listar
- SP1: REST completo + Angular consume todo

## Levantar (orden)
1. `cd database && docker compose --env-file .env up -d` (MySQL 5510, phpMyAdmin 3410)
2. Mensajería (T1, opcional pero recomendado para demo completa):
   - `cd DESARROLLO-DE-APLICACIONES-WEB-II/queue && docker compose -f docker-compose-kafka.yml up -d` (9092)
   - `docker compose -f docker-compose-rabbitmq.yml up -d` (5672,15672 guest/guest)
   - Sin broker igual funciona: ventas guarda PENDING + total por Feign, inventario descuenta al consumir
3. Infra descubrimiento (T1 Spring Cloud):
   - `cd infra/eureka-server && ./mvnw spring-boot:run` (8761, dashboard http://localhost:8761)
   - `cd infra/api-gateway && ./mvnw spring-boot:run` (8762)
4. Backend (3 terminales IntelliJ, Gradle):
   - `cd backend/auth-service && ./gradlew bootRun` (8083, seed admin/admin123 ADMIN, vendedor/vendedor123 VENDEDOR, se registra en Eureka)
   - `cd backend/inventario-service && ./gradlew bootRun` (8081, seed 3 joyas)
   - `cd backend/ventas-service && ./gradlew bootRun` (8082, Feign lb://inventario-service)
5. Frontend (WebStorm):
   - `cd frontend/joyeria-front && npm install && ng serve` (4200)
   - Login admin/admin123 -> /inventario -> /ventas (directo 8081/8082, o vía gateway 8762 cambiando environment a http://localhost:8762)
6. Postman: importar `postman/Joyeria-Flows.postman_collection.json`

## Puertos y BD
- auth:8083, inventario:8081, ventas:8082, mysql:5510/joyeria_db, front:4200
- Tablas: `joyas`, `ventas`, `app_users`, `customers`, `secure_sales`

## Microservicios (clase)
- Sync: ventas -> inventario por Feign `GET /products/{id}` (`client/ProductClient.java`), `GET /sales/{id}/details` calcula total
- Async: ventas publica `stock-movements` (Kafka) o `stock.reserve` (Rabbit), inventario `decreaseStock()` + alerta `stock.low`
- Seguridad: JWT solo en auth:8083 (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `BCryptPasswordEncoder`), CORS a 4200
