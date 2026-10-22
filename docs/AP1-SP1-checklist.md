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
- [x] Feign ventas->inventario, RabbitMQ `stock.reserve`, Kafka `stock-movements`
- [x] JWT `auth-service:8083`, CORS 4200, Docker MySQL 5510/joyeria_db
- [ ] Opcional: Eureka 8761 + Gateway 8762 (spring_cloud del curso), Resiliencia, K8s

## Cómo demostrar
1. `database/docker compose up -d` -> joyeria_db creada
2. `./gradlew test` (3 servicios) -> mostrar BUILD SUCCESSFUL
3. `bootRun` 8083,8081,8082 -> Postman `Joyeria-Flows` login -> JWT -> CRUD
4. `ng serve` 4200 -> login admin/admin123 -> Inventario CRUD -> Ventas crear -> Detalle Feign
