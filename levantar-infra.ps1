# Joyería Morgan - Levanta toda la infra Docker en un solo paso
# Uso: powershell -ExecutionPolicy Bypass -File "C:\Users\David\Desktop\Proyecto Final\joyeria-morgan-final\levantar-infra.ps1"
# Levanta: MySQL 5510 + phpMyAdmin 3410 + RabbitMQ 5672/15672 + Kafka 9092 + Kafka-UI 8090
$ROOT = "C:\Users\David\Desktop\Proyecto Final\joyeria-morgan-final\database"
$QUEUE = "C:\Users\David\Desktop\Proyecto Final\DESARROLLO-DE-APLICACIONES-WEB-II\queue"

Write-Host "== 1/3 MySQL ==" -ForegroundColor Cyan
docker compose --env-file "$ROOT\.env" -f "$ROOT\docker-compose.yml" up -d
Write-Host "== 2/3 RabbitMQ ==" -ForegroundColor Cyan
docker compose -f "$QUEUE\docker-compose-rabbitmq.yml" up -d
Write-Host "== 3/3 Kafka ==" -ForegroundColor Cyan
docker compose -f "$QUEUE\docker-compose-kafka.yml" up -d

Write-Host "Esperando 25s a que arranquen..." -ForegroundColor Yellow
Start-Sleep 25

$estado = docker ps -a --format "{{.Names}} {{.Status}}"
$estado | Select-String "joyeria|ligo"
if ($estado | Select-String "ligo-kafka Exited") {
  Write-Host "Kafka caido (dato viejo en Zookeeper). Reparando..." -ForegroundColor Red
  docker compose -f "$QUEUE\docker-compose-kafka.yml" down
  docker compose -f "$QUEUE\docker-compose-kafka.yml" up -d
  Start-Sleep 30
  docker ps --format "{{.Names}} {{.Status}}" | Select-String "ligo"
}

Write-Host "== Verificacion final ==" -ForegroundColor Green
docker ps --format "{{.Names}} {{.Status}} {{.Ports}}" | Select-String "joyeria|ligo"
