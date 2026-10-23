# Checklist AP1 (Semana 11 - 50%) y SP1 (Semana 14 - 100%)

## AP1 - 20 pts
### Formal 8 pts
- [ ] Diagnóstico SEPTE (Social, Económica, Tecnológica) Joyería Morgan
- [ ] Objetivos SMART (2)
- [ ] Justificación + beneficiarios directos/indirectos
- [ ] Conclusiones (máx 3)

### Técnica 12 pts
- [x] Login REST sin errores + validaciones + BCrypt
  - `auth-service/rest/AuthController.java POST /auth/login`
  - `auth-service/negocio/AuthServiceTest.java loginOk/loginFalla/passwordCifradoBCrypt`
- [x] Pruebas DAO insertar/actualizar/eliminar/listar
  - `inventario-service/.../ProductRepositoryTest.java` 3 tests OK
  - `ventas-service/.../SaleRepositoryTest.java` 3 tests OK
  - Ejecutar: `./gradlew test` en cada servicio -> BUILD SUCCESSFUL

## SP1 - 20 pts (60% informe + 40% sustentación)
### Técnica 12 pts
- [x] REST GET,POST,PUT,DELETE
  - Inventario: `GET/POST /products`, `GET/PUT/DELETE /products/{id}`
  - Ventas: `GET/POST /sales`, `GET/PUT/DELETE /sales/{id}`, `GET /sales/{id}/details` (Feign)
- [x] Angular consume todo
  - `frontend/joyeria-front/src/app/services/joya.ts`, `venta.ts`
  - `components/inventario/`, `components/ventas/`, `services/auth.ts` JWT + interceptor
  - Build: `ng build` OK (dist/joyeria-front)

## Microservicios extra (clase)
- [x] Feign ventas->inventario, RabbitMQ `stock.reserve`, Kafka `stock-movements` + compensacion `sale-cancellation-requests`
- [x] JWT en los 5 servicios: 401 sin token / 403 por rol, CORS 4200, Docker MySQL 5510/joyeria_db
- [x] Eureka 8761 + Config Server 8888 + Gateway 8762 + Resilience4j (CircuitBreaker/Retry) + K8s

## Como demostrar
1. `docker compose -f docker-compose.full.yml up -d --build` -> MySQL + Rabbit + Kafka + Eureka + Config + backends + gateway + frontend
2. `./gradlew test` en backend o `npm test` en `frontend/joyeria-front` (5/5) -> BUILD SUCCESSFUL
3. `ng serve` 4200 -> login admin/admin123 -> Inventario CRUD -> Ventas crear -> Detalle Feign
4. Postman `Joyeria-Morgan-DEMO` (38 peticiones): 401/403, 404/400/409, Kafka/Rabbit, anulacion con devolucion de stock
