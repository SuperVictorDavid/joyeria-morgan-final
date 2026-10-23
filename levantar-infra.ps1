# Joyeria Morgan - Levanta toda la infraestructura Docker en un solo paso.
# Uso: powershell -ExecutionPolicy Bypass -File .\levantar-infra.ps1
#      powershell -ExecutionPolicy Bypass -File .\levantar-infra.ps1 -ConTodo
#
# Sin parametros levanta solo la infraestructura: MySQL 5510 + RabbitMQ 5672/15672
# + Kafka 9092 + Kafka-UI 8090 (los servicios se corren desde IntelliJ).
# Con -ConTodo levanta ademas Eureka, Config Server, los 5 backends, el gateway
# y el frontend en 4200 (todo compilado dentro de Docker).

param(
    [switch]$ConTodo
)

$ROOT = $PSScriptRoot
$COMPOSE = Join-Path $ROOT "docker-compose.full.yml"

if (-not (Test-Path $COMPOSE)) {
    Write-Host "No se encontro docker-compose.full.yml en $ROOT" -ForegroundColor Red
    exit 1
}

if ($ConTodo) {
    Write-Host "== Levantando stack completo (incluye frontend y backends) ==" -ForegroundColor Cyan
    docker compose -f $COMPOSE up -d --build
} else {
    Write-Host "== 1/2 MySQL + RabbitMQ + Kafka + Kafka-UI ==" -ForegroundColor Cyan
    docker compose -f $COMPOSE up -d mysql rabbitmq kafka kafka-ui
}

Write-Host "== Esperando 25s a que arranquen ==" -ForegroundColor Yellow
Start-Sleep 25

Write-Host "== Verificacion final ==" -ForegroundColor Green
docker ps --format "{{.Names}} {{.Status}} {{.Ports}}" | Select-String "joyeria"

Write-Host ""
Write-Host "MySQL        : localhost:5510  (joyeria_db, app/password)" -ForegroundColor Gray
Write-Host "RabbitMQ     : localhost:15672 (guest/guest)" -ForegroundColor Gray
Write-Host "Kafka UI     : http://localhost:8090" -ForegroundColor Gray
Write-Host "Eureka       : http://localhost:8761" -ForegroundColor Gray
Write-Host "Config Server: http://localhost:8888" -ForegroundColor Gray
