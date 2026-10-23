# Joyería Morgan - Proyecto Final DAW II (4697)

Negocio rescatado de `JoyeriaMorgaMVC` (ASP.NET MVC): inventario de joyas + ventas con carrito/boleta.
Técnica 100% del repo del curso `DESARROLLO-DE-APLICACIONES-WEB-II`: microservicios Spring Boot 3.2.5 Java 17 + JWT + Feign + RabbitMQ/Kafka + Eureka + Config Server + Gateway + Angular.

## Estructura
```
backend/auth-service:8083       login JWT BCrypt, users (solo auth)
backend/clientes-service:8084   clientes CRUD GET/POST/PUT/DELETE
backend/inventario-service:8081 joyas CRUD + Rabbit/Kafka descuenta y devuelve stock
backend/ventas-service:8082     ventas CRUD + Feign lb://inventario + CB/Retry + Kafka/Rabbit
backend/reportes-service:8085   agregador GET /reports/resumen|inventario|ventas (Feign a los 3)
infra/eureka-server:8761        descubrimiento (T1)
infra/config-server:8888        configuracion centralizada (native config-repo/)
infra/api-gateway:8762          rutas lb://auth,clientes,inventario,ventas,reportes + CORS 4200
frontend/joyeria-front:4200     Angular 22 login + guard + interceptor + dashboard + productos + clientes + historial + carrito + reportes PDF
database/                       docker-compose MySQL 5510 joyeria_db + script.sql
docker-compose.full.yml         stack completo: MySQL + RabbitMQ + Kafka + Kafka-UI + Eureka + Config + 5 backends + Gateway + Frontend
postman/                        Coleccion DEMO lista para importar (39 peticiones) + README
k8s/                            manifiestos de despliegue (NodePort)
```

## U2 Docker/K8s
- Dockerfiles **multi-stage**: compilan dentro de la imagen (`gradlew bootJar` o `mvn package`), no necesitan `build/` ni `target/` en la maquina.
- Full: `docker compose -f docker-compose.full.yml up -d --build`
  - MySQL 5510 (host) / 3306 (red), RabbitMQ 5672 + panel 15672, Kafka 9092 (interno 29092), Kafka-UI 8090.
  - Eureka 8761, Config Server 8888, backends 8081-8085, Gateway 8762, Frontend 4200.
  - Dentro de la red los hosts son nombres de servicio (`mysql`, `kafka`, `eureka-server`, ...) y se inyectan por variables de entorno.
- Infra solamente (para correr los servicios desde IntelliJ): `powershell -ExecutionPolicy Bypass -File .\levantar-infra.ps1`
- Stack completo: `powershell -ExecutionPolicy Bypass -File .\levantar-infra.ps1 -ConTodo`
- K8s: `k8s/*-deployment.yaml` + `*-service.yaml` (NodePort 30081-30085, 30061-30062, 30088, 30306). Aplicar: `kubectl apply -f k8s/`

## Mapeo Examen T1 -> Proyecto
- Feign Client: `ventas-service/client/ProductClient.java` name=`inventario-service` (por Eureka) + `GET /sales/{id}/details` y `GET /sales/{id}/details-resiliente` (CB)
- RabbitMQ: `stock-reserve-queue`/`stock-exchange` en ambos servicios, `POST /sales/rabbit-reserve`, `StockReserveConsumer.decreaseStock()`, alerta `stock.low` si <5
- Kafka: topic `stock-movements`, `POST /sales` publica `StockMovementEvent`, `StockUpdateConsumer` descuenta
- Kafka compensacion: al `DELETE /sales/{id}` se publica `sale-cancellation-requests` y `SaleCancellationConsumer` devuelve el stock
- Spring Cloud: Eureka 8761 + Config Server 8888 + Gateway 8762; los 6 modulos Spring consumen `optional:configserver:${CONFIG_SERVER_URL}`
- Seguridad (Unidad 2): JWT `auth-service/security/*`, BCrypt, `POST /auth/login`
- Resiliencia/Docker/K8s (clase): `resiliencia-demo`, `spring_cloud`, `queue/` como referencia

## Requisitos rúbrica
- Login REST con BCrypt: `POST /auth/login {username,password} -> {token}` `auth-service/security/*`
- Angular consume GET,POST,PUT,DELETE y persiste: `services/joya.ts`, `services/venta.ts`, `components/inventario`, `components/ventas`
- AP1: login sin errores + pruebas DAO insertar/actualizar/eliminar/listar
- SP1: REST completo + Angular consume todo

## Levantar (orden)

### Opcion A - todo con Docker (recomendada para exponer)
```powershell
docker compose -f docker-compose.full.yml up -d --build
```
Esperar ~2 min. Verificar `docker ps` y abrir:
- Frontend http://localhost:4200 (`admin/admin123` o `vendedor/vendedor123`)
- Eureka http://localhost:8761 · Config Server http://localhost:8888/api-gateway/default
- Kafka-UI http://localhost:8090 · RabbitMQ http://localhost:15672 (guest/guest)

### Opcion B - infra en Docker + servicios en IntelliJ
1. `powershell -ExecutionPolicy Bypass -File .\levantar-infra.ps1` (MySQL + RabbitMQ + Kafka + Kafka-UI)
2. Arrancar en este orden: `infra/eureka-server`, `infra/config-server`, los 5 `backend/*`, `infra/api-gateway`.
3. Frontend: `cd frontend/joyeria-front && npm install && npm start` (usa `environment.development.ts`, puertos directos).

> Si el Config Server no esta arriba, cada servicio arranca igual con su `application.yml` local: el import es `optional:`.

> **Si Kafka aparece como `Exited (1)`**: en sus logs sale `KeeperException$NodeExistsException: KeeperErrorCode = NodeExists`, un nodo viejo `/brokers/ids/1` que quedo de un apagado brusco. Solucion: esperar ~30 s (Zookeeper expira la sesion) y pulsar Start en `ligo-kafka`; si insiste, `docker exec ligo-zookeeper zookeeper-shell localhost:2181 delete /brokers/ids/1` y arrancarlo de nuevo. Que Kafka-UI (8090) este arriba **no** significa que Kafka este arriba: revisar que `ligo-kafka` diga `Up` y que 9092 acepte conexiones. Sin Kafka, `POST /sales` responde 503 "Kafka no disponible" y no guarda la venta (asi no se pierde el stock).
>
> Los contenedores `ligo-zookeeper`, `ligo-kafka` y `ligo-kafka-ui` ya tienen `restart: unless-stopped`, y el `docker-compose.full.yml` del proyecto tambien: si apagas el PC de golpe, Docker los vuelve a levantar y Kafka se reintenta solo hasta que Zookeeper libera la sesion vieja. Aun asi, antes de exponer confirma que `ligo-kafka` este `Up`.
>
> El RabbitMQ que publica puertos es `ligo-rabbitmq` (5672/15672, guest/guest); el contenedor suelto `rabbitmq` no expone puertos y no lo usa el proyecto.

## Puertos y BD
- auth:8083, inventario:8081, ventas:8082, clientes:8084, reportes:8085, gateway:8762, config:8888, eureka:8761, mysql:5510, rabbit:5672/15672, kafka:9092, kafka-ui:8090, front:4200
- Tablas: `products`, `sales`, `app_users`, `customers`

## Frontend: desarrollo vs produccion
- `environment.development.ts` (lo usa `ng serve`): cada servicio directo en `localhost:8081..8085`.
- `environment.ts` (lo usa `ng build`): `apiUrl: '/api'`; nginx del contenedor proxya `/api/*` hacia `api-gateway:8762`. El bundle de produccion no lleva hosts ni puertos de microservicios.
- `npm test` (Vitest) y `npm run build` deben quedar en verde (5/5 tests).

## Demo publica temporal (tunel)

Para que tu grupo abra la app desde cualquier red **mientras tu laptop esta encendida**, sin pagar nube:
se sirve el build de produccion en un contenedor nginx y se expone con un tunel de Cloudflare.

Requisitos (una sola vez):
```powershell
docker build -t joyeria-front:demo .\frontend\joyeria-front
```
`cloudflared` queda en `%LOCALAPPDATA%\cloudflared\cloudflared.exe` (si falta, descargarlo de
https://github.com/cloudflare/cloudflared/releases).

Cada vez que quieras exponer:
1. Docker Desktop encendido.
2. En IntelliJ: Eureka, Config Server, los 5 microservicios y el API Gateway (`8762`).
3. Doble clic a **`DEMO-TUNEL.bat`**: arranca el contenedor `joyeria-front-demo` (puerto `4201`) y abre el tunel.
4. Copia la linea `https://xxxx-xxxx-xxxx.trycloudflare.com` y enviala al grupo.
5. Para cortar: **`DETENER-TUNEL.bat`** o `Ctrl+C` en la ventana.

Notas:
- La URL cambia cada vez que se reinicia el tunel; el `.bat` siempre muestra la nueva.
- La laptop debe quedar encendida y **sin suspender**; si se duerme, la URL deja de responder.
- Solo se publica el frontend (`4201`) y su proxy `/api`; MySQL, Kafka y RabbitMQ no quedan expuestos.
- El login de la demo es `admin/admin123` (o `vendedor/vendedor123`): cualquiera con el enlace puede entrar, es temporal.
- `nginx.conf` quita el header `Origin` hacia el gateway para que el CORS de `localhost:4200` no bloquee el dominio del tunel.

## Coleccion Postman
- `postman/Joyeria-Morgan-DEMO.postman_collection.json`: 39 peticiones, 9 carpetas, un solo archivo para importar.
- Sin environment (las variables viven en la coleccion). Detalle en `postman/README-coleccion.md`.
- Verifica: 401 sin token, 403 por rol, 404/400/409 reales, total por Feign, stock por Kafka/Rabbit y devolucion de stock al anular.

## Microservicios (clase)
- Sync: ventas -> inventario por Feign `GET /products/{id}` (`client/ProductClient.java`), `GET /sales/{id}/details` calcula total
- Async: ventas publica `stock-movements` (Kafka) o `stock.reserve` (Rabbit), inventario `decreaseStock()` + alerta `stock.low`; al anular se publica `sale-cancellation-requests` y se hace `increaseStock()`
- Seguridad: JWT en los 5 servicios (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `BCryptPasswordEncoder`); 401 sin token, 403 por rol; CORS a 4200
- Config: `infra/config-server/config-repo/*.yml` es la fuente central; los servicios la importan y aceptan override por variables de entorno
