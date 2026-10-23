# Colección Postman — Joyería Morgan

## Un solo archivo

| Archivo | Uso |
|---|---|
| `Joyeria-Morgan-DEMO.postman_collection.json` | Colección completa para importar y exponer: **39 peticiones, 9 carpetas, ~6 s**, 0 fallos |

No hay que importar ningún environment: **todas las URLs y variables están dentro de la colección**.

## Importar y ejecutar

1. Postman → **Import** → **File** → `postman/Joyeria-Morgan-DEMO.postman_collection.json`.
2. Abrir la colección → botón **Run** (Run collection).
3. El orden de las carpetas ya está definido; no cambies nada.

## Requisitos

- Infraestructura arriba (Docker: MySQL 5510, RabbitMQ 5672, Kafka 9092, Kafka-UI 8090).
- Eureka 8761, Config Server 8888, los 5 backends (8081-8085) y el gateway 8762.
- Usuarios semilla: `admin/admin123` (ADMIN) y `vendedor/vendedor123` (VENDEDOR).

## Resultado esperado

- **39 requests · 0 fallos · ~6 s**. Cada petición tiene tests automáticos: si algo del sistema se rompe, la colección lo marca en rojo.

## Contenido por carpeta

| Carpeta | Qué demuestra |
|---|---|
| `01. Seguridad y roles (T4)` | Login admin/vendedor, estructura del JWT, **401 sin token**, 403 por rol insuficiente, listar usuarios con ADMIN |
| `02. CRUD inventario (8081)` | GET/POST/PUT/DELETE de joyas, **buscar una joya por ID**, **404 producto inexistente**, **400 precio inválido**, **400 stock negativo** |
| `03. CRUD clientes (8084)` | GET/POST/PUT/DELETE de clientes |
| `04. Ventas: Feign + Kafka (T1/T2)` | Total calculado por **Feign**, venta publicada en **Kafka**, detalle con Feign + **404 producto inexistente**, **400 cantidad negativa**, **409 stock insuficiente** |
| `05. Resiliencia (T5)` | `GET /sales/{id}/details-resiliente` (CircuitBreaker + Retry) y estado del circuito |
| `06. RabbitMQ y stock final (T2)` | Venta con reserva RabbitMQ (con `total` y `fechaVenta`), colas `stock-reserve-queue` / `stock-low-queue`, stock tras Kafka + Rabbit |
| `07. Eureka, gateway y reportes (T3/T1)` | Servicios registrados en Eureka, productos vía gateway `lb://`, resumen de reportes |
| `08. Observabilidad (T7)` | Health y métricas de actuator |
| `09. Limpieza` | Anula las ventas (Kafka devuelve el stock), verifica stock final 100 y borra la joya temporal |

## Cobertura de temas

| Tema | Dónde se demuestra |
|---|---|
| T1 Feign / Spring Cloud | `04` (total y detalle por Feign), `07` (Eureka, gateway con `lb://`, Config Server) |
| T2 Kafka | `04` venta publica `stock-movements`, `08` no aplica, evidencia en Kafka-UI |
| T2 RabbitMQ | `06` reserva en `stock-reserve-queue` y alerta `stock-low-queue` |
| T3 Config Server | `07` y arranque de servicios con `spring.config.import` |
| T4 Seguridad JWT y roles | Login, 401 sin token, 403 por rol, `@PreAuthorize` |
| T5 Resiliencia | `05` CircuitBreaker + Retry + fallback |
| T6 Docker/K8s | Evidencia de contenedores vía API de RabbitMQ y Kafka-UI |
| T7 Observabilidad | `08` Health/Prometheus/metrics |

## Autorización por roles (ADMIN vs VENDEDOR)

Implementada con `@EnableMethodSecurity` + `@PreAuthorize`. El rol viaja en el JWT (`role`) y el filtro lo convierte en `ROLE_ADMIN` / `ROLE_VENDEDOR`.

| Recurso | ADMIN | VENDEDOR |
|---|---|---|
| `GET /products`, `GET /products/{id}` | ✅ | ✅ |
| `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}` | ✅ | ❌ 403 |
| `GET /customers`, `POST /customers`, `PUT /customers/{id}` | ✅ | ✅ |
| `DELETE /customers/{id}` | ✅ | ❌ 403 |
| `GET /sales`, `POST /sales`, `PUT /sales/{id}` | ✅ | ✅ |
| `DELETE /sales/{id}` | ✅ | ❌ 403 |
| `GET /reports/**` | ✅ | ❌ 403 |
| `GET/POST/PUT/DELETE /users` | ✅ | ❌ 403 |

En Angular: el enlace **Reportes** se oculta y su ruta está protegida con `adminGuard`; los botones **Nuevo/Editar/Eliminar producto**, **Eliminar cliente** y **Eliminar venta** solo aparecen al ADMIN.

> Nota: `POST /users` dejó de ser público. Antes cualquiera podía crear un usuario con `role: ADMIN` sin token.

> Si cambias el backend hay que **reiniciar los 5 servicios** (auth, clientes, inventario, ventas, reportes) para que tomen el código nuevo.

## Datos de prueba y re-ejecución

- Cada corrida genera un `runId` y crea: una joya `Joya QA <runId>`, un cliente `Cliente QA <runId>`, una joya de venta con stock 100 y dos ventas.
- Las ventas usan **solo** joyas temporales, así que el stock del catálogo real no se consume.
- `09. Limpieza` anula primero las ventas (Kafka devuelve el stock), verifica que el stock volvió a 100 y recién elimina la joya. La BD queda como estaba: solo se crean y borran los datos de esa corrida.
- Si ejecutas solo una parte, las peticiones de limpieza se omiten solas.

## Comportamientos esperados (no son fallos)

- `Login con clave incorrecta` y `Acceso sin token` devuelven **401**; **403** queda solo para un rol insuficiente.
- `Detalle resiliente` devuelve **200**, o **503** si el circuito está OPEN (se recupera en 30 s).
- El stock tarda milisegundos en descontarse porque lo hacen consumidores asíncronos (Kafka/Rabbit): la verificación acepta 97 u 98 y la de anulación espera hasta 4 s.
- Si Kafka está caído, `POST /sales` responde **503** y no guarda la venta.
- Si inventario está caído, `POST /sales` y los reportes responden **503** (antes 200 con datos en cero).

## Correcciones aplicadas al backend

1. Los 5 `SecurityConfig` permiten `/error` y definen `authenticationEntryPoint` (401) y `accessDeniedHandler` (403): ya no se enmascara ningún 404/400/409 como 403.
2. Validación con Jakarta (`@Valid` + `ProductRequest` / `SaleRequest`): precio > 0, stock >= 0, cantidad > 0.
3. `POST /sales` valida producto y stock por Feign, calcula el total y solo responde 200 si Kafka confirmó el evento; si no, 503 y no deja la venta.
4. `POST /sales/rabbit-reserve` guarda `total` y `fechaVenta` igual que la venta Kafka.
5. `PUT /sales/{id}` recalcula el total y ajusta el stock.
6. `DELETE /sales/{id}` publica `sale-cancellation-requests` e inventario devuelve el stock antes de borrar la fila.
7. `decreaseStock` rechaza cantidades nulas, cero o negativas (antes `quantity: -5` sumaba stock).
8. Reportes responde 503 con el motivo cuando una dependencia falla.
9. `auth-service` ya no duplica `Customer`/`Sale`: esos datos viven solo en clientes y ventas.
10. Los servicios consumen Config Server (`spring.config.import: optional:configserver:...`); si no está arriba arrancan con su `application.yml` local.

## Ejecución por consola (opcional)

```powershell
npx --yes newman run "postman/Joyeria-Morgan-DEMO.postman_collection.json" --reporters cli
```
