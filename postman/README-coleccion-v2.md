# Colección Postman v2 — Joyería Morgan

## Dos colecciones

| Archivo | Uso | Tamaño |
|---|---|---|
| `Joyeria-Morgan-DEMO.postman_collection.json` | **Para la exposición** (10 min): 31 peticiones, 9 carpetas | ~4 s por corrida |
| `Joyeria-Morgan-v2.postman_collection.json` | Versión completa / respaldo y evidencia: 74 peticiones, 11 carpetas | ~8 s por corrida |

La DEMO usa las mismas peticiones y tests que la completa; solo quita las repetitivas. Si el tiempo aprieta se pueden saltar las carpetas 02, 03 y 08 sin romper la corrida.

## Importar

1. Postman → **Import** → **File** → seleccionar:
   `postman/Joyeria-Morgan-DEMO.postman_collection.json` (para la demo) o
   `postman/Joyeria-Morgan-v2.postman_collection.json` (completa).
2. **No hace falta importar ningún environment**: todas las URLs y variables están dentro de la colección.
3. Ejecutar la colección completa con **Run collection** (respeta el orden de las carpetas).

## Requisitos

- Docker arriba (`levantar-infra.ps1`).
- Eureka, config-server, los 5 backends y el gateway arriba.
- Usuarios semilla: `admin/admin123` (ADMIN) y `vendedor/vendedor123` (VENDEDOR).

## Resultado esperado

- DEMO: 31 requests · 67 aserciones · 0 fallos · ~4 s.
- Completa: 74 requests · 152 aserciones · 0 fallos · ~8 s.

Cada request tiene tests automáticos: si algo del sistema se rompe, la colección lo marca en rojo.

## Cobertura de temas

| Tema | Dónde se demuestra |
|---|---|
| T1 Feign | `04. Detalle con Feign`, cálculo del total al crear la venta, `06. Detalle Feign por gateway` |
| T2 Kafka | `04. Crear venta - publica en Kafka`, `08. Kafka UI - Tópico stock-movements` |
| T2 RabbitMQ | `04. Crear venta con reserva RabbitMQ`, `08. RabbitMQ - Cola stock-reserve-queue / stock-low-queue` |
| T3 Spring Cloud | Eureka (panel, apps, instancia UP), Config Server, gateway con `lb://` |
| T4 Seguridad JWT | Login admin/vendedor, estructura y expiración del JWT, acceso sin token y con token inválido |
| T4 Autorización por roles | `01b. Autorización por roles (T4)`: el vendedor lista joyas (200) pero recibe 403 al crear/editar productos, ver reportes, listar usuarios o crear un ADMIN; también se crea un vendedor nuevo (`qa_<runId>`) y se comprueba que nace limitado; `04. VENDEDOR - Eliminar venta (403)` |
| T5 Resiliencia | `04. Detalle resiliente (CircuitBreaker + Retry)`, `07. Circuit Breaker - estado / eventos` |
| T6 Docker/K8s | Evidencia de contenedores vía API de RabbitMQ y Kafka-UI (Docker/K8s no se prueba desde Postman) |
| T7 Observabilidad | `07. Health`, `Prometheus`, `metrics/http.server.requests`, eventos de Resilience4j |

## Autorización por roles (ADMIN vs VENDEDOR)

Implementada en el backend con `@EnableMethodSecurity` + `@PreAuthorize` en los controladores. El rol viaja en el JWT (`role`) y el filtro lo convierte en `ROLE_ADMIN` / `ROLE_VENDEDOR`.

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

Archivos tocados: `ProductController`, `CustomerController` (clientes), `SaleController` (ventas), `ReporteController`, `UserController` y los 5 `SecurityConfig`.

En Angular se refleja así: el enlace **Reportes** se oculta y su ruta queda protegida con `adminGuard`; los botones **Nuevo/Editar/Eliminar producto**, **Eliminar cliente** y **Eliminar venta** solo se muestran al ADMIN. El vendedor sigue pudiendo vender, registrar/editar clientes y ver el catálogo.

> Nota: `POST /users` dejó de ser público. Antes cualquiera podía crear un usuario con `role: ADMIN` sin token.

> Tras cambiar el backend hay que **reiniciar los 5 servicios** (auth, inventario, clientes, ventas, reportes) para que tomen el código nuevo. El token viejo sigue siendo válido hasta expirar (60 min), pero el rol que trae ya no sirve para más de lo permitido: la autorización se evalúa en cada petición.

## Datos de prueba y re-ejecución

- Cada corrida genera un `runId` único y crea: una joya `Joya QA <runId>`, un cliente `Cliente QA <runId>` (DNI `9<runId>`), un usuario `qa_<runId>` y una joya de venta con stock 100.
- Las ventas usan **solo** esas joyas temporales, así que el stock del catálogo real no se consume.
- La carpeta `09. Limpieza` elimina todo lo creado. La BD queda igual: 27 joyas, 21 ventas, 13 clientes, 5 usuarios.
- Si ejecutas solo una parte, las peticiones de limpieza se omiten solas.

## Comportamientos esperados (no son fallos)

- `Login con clave incorrecta` y `Acceso sin token` devuelven **403**, no 401.
- `Detalle resiliente` devuelve **200**, o **503** si el circuito está OPEN (se recupera en 30 s).
- El stock tarda milisegundos en descontarse porque lo hacen consumidores asíncronos (Kafka/Rabbit).

## Problemas del backend detectados (documentados, no ocultos)

1. Cualquier error (404, 400, 500) se enmascara como **403** porque `SecurityConfig` no permite `/error`.
2. `auth-service` no expone `/actuator/**` (los otros 4 servicios sí).
3. `POST /sales/rabbit-reserve` guarda `total: null` y `fechaVenta: null`.
4. `POST /sales` acepta un `productId` inexistente y guarda la venta con `total: null`.

## Ejecución por consola (opcional)

```powershell
npx newman run "postman/Joyeria-Morgan-v2.postman_collection.json" --reporters cli
```
