🔧 Полный сброс и пересборка
docker compose down -v        # остановить и удалить контейнеры, сети, тома
docker system prune -af       # удалить неиспользуемые образы, контейнеры и кэш
docker compose build --no-cache
docker compose build
docker compose up -d

API Gateway
# Список маршрутов (ожидаем непустой JSON)
curl -s http://localhost:2021/actuator/gateway/routes | jq

Здоровье сервисов ЧЕРЕЗ gateway
# пример: сервисы, проброшенные как маршруты через gateway
curl -s http://localhost:2021/airport-pilot-service/actuator/health
curl -s http://localhost:2021/airport-bookings-service/actuator/health

Здоровье сервисов НАПРЯМУЮ (минуя gateway)
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health
curl -s http://localhost:8085/actuator/health
curl -s http://localhost:8086/actuator/health

# Проверка, кого видит Eureka
curl -s http://localhost:8761/eureka/apps | jq

