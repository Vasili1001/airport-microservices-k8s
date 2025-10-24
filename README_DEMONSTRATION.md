## 1) Eureka — реестр сервисов

### Что демонстрируем:

Все микросервисы зарегистрированы и видят друг друга через Service Discovery.
Без реестра ни балансировка, ни маршрутизация через Gateway не работают.

### Роль в системе:

Для сотрудников/пассажиров — это “невидимая инфраструктура”, обеспечивающая отказоустойчивость и масштабирование: сервисы можно перезапускать/масштабировать, а остальные автоматически находят их по имени.

Команды:
Веб-панель со статусами UP
open http://localhost:8761/  # или вручную в браузере

JSON со списком всех приложений
curl -s -H 'Accept: application/json' http://localhost:8761/eureka/apps | jq

## ================================================================================================

## 2) API Gateway — входная точка

### Что демонстрируем:

Gateway подтянул маршруты из Eureka и применяет правила RewritePath.
Любой клиент ходит только на 2021 порт — централизованная аутентификация/логирование/квоты и т.п.

### Роль в системе:

Единая точка входа для мобильных приложений пассажиров и внутренних панелей сотрудников.
Упрощает безопасность и скрывает внутреннюю топологию.

Команда:
curl -s http://localhost:2021/actuator/gateway/routes | jq

В списке будут кастомные маршруты:

/airport-pilot-service/** → AIRPORT-PILOT-SERVICE
/airport-flight-service/** → AIRPORT-FLIGHT-SERVICE
/airport-bookings-service/** → AIRPORT-BOOKINGS-SERVICE
/airport-security-service/** → AIRPORT-SECURITY-SERVICE
/airport-checkin-service/** → AIRPORT-CHECKIN-SERVICE
/airport-boarding-service/** → AIRPORT-BOARDING-SERVICE

## ================================================================================================

## 3) Сквозная доступность КАЖДОГО сервиса ЧЕРЕЗ Gateway

### Что демонстрируем:

Запрос идёт на Gateway, тот через Eureka находит сервис по lb://... и проксирует.

От каждого сервиса получаем HTTP 200 — значит вся цепочка работает.

### Общие зависимости сервисов:

Все доменные сервисы используют MySQL.

Все зарегистрированы в Eureka и доступны через API Gateway.

### Команды и пояснения:
#### 3.1 Pilot service — пилоты и экипажи
curl -i http://localhost:2021/airport-pilot-service/actuator/health
#### Роль: справочник пилотов/экипажей — нужен сотрудникам расписаний и безопасности.
#### Связи: читает/пишет в MySQL; может использоваться Flight/Bookings для справок по экипажам.

#### 3.2 Flight service — рейсы и расписание
curl -i http://localhost:2021/airport-flight-service/actuator/health
#### Роль: расписание рейсов, статусы (по расписанию/задержан/прибыл).
#### Польза: пассажиры видят статус своего рейса; сотрудники — планирование стоек, гейтов.
#### Связи: MySQL; может запрашиваться Bookings/Check-in/Boarding.

#### 3.3 Bookings service — бронирования/билеты
curl -i http://localhost:2021/airport-bookings-service/actuator/health
#### Роль: управление бронированиями, привязка пассажира к рейсу/месту.
#### Польза: пассажиры — покупка/управление; сотрудники — верификация брони.
#### Связи: MySQL; взаимодействует логически с Flight (рейс) и Check-in (регистрация).

#### 3.4 Security service — служба безопасности
curl -i http://localhost:2021/airport-security-service/actuator/health
#### Роль: проверки пассажиров/документов, статусы прохождения контроля.
#### Польза: сотрудникам — контроль допуска; пассажирам — прозрачность статуса.
#### Связи: MySQL; может читать данные бронирования/персоналии из Bookings.

#### 3.5 Check-in service — регистрация на рейс
curl -i http://localhost:2021/airport-checkin-service/actuator/health
#### Роль: выбор места, получение посадочного талона.
#### Польза: пассажирам — онлайн/офлайн check-in; сотрудникам — управление стойками.
#### Связи: Bookings (подтверждение билета), Flight (параметры рейса), MySQL.

#### 3.6 Boarding service — посадка на рейс у гейта
curl -i http://localhost:2021/airport-boarding-service/actuator/health
#### Роль: сканирование посадочного, статусы boarding/open/closed.
#### Польза: сотрудникам у гейта — посадка по спискам; пассажирам — статус выполнения.
#### Связи: Check-in (посадочный), Flight (статус), Security (допуск), MySQL.

## ================================================================================================

## 4) Сравнение: прямой доступ (минуя Gateway)

### Что демонстрируем:

Те же сервисы живы на своих портах — но в реальной жизни ходят через Gateway.

curl -i http://localhost:8081/actuator/health  # pilot
curl -i http://localhost:8082/actuator/health  # flight
curl -i http://localhost:8083/actuator/health  # bookings
curl -i http://localhost:8084/actuator/health  # security
curl -i http://localhost:8085/actuator/health  # checkin
curl -i http://localhost:8086/actuator/health  # boarding

## ================================================================================================

## 5) MySQL — схема и данные

### Что демонстрируем:

База поднята, схема инициализирована, сервисы реально коннектятся (в health видно db: UP).

### Роль в системе:

Единое хранилище транзакционных данных: рейсы, брони, статусы прохождения процессов.

### Команды:

#### Подключиться и показать БД/таблицы
docker exec -it mysql mysql -uroot -proot -e "SHOW DATABASES; USE airportdb; SHOW TABLES;"

docker exec -it mysql mysql -uroot -proot -D airportdb -e "SELECT COUNT(*) AS pilots FROM pilots;"
docker exec -it mysql mysql -uroot -proot -D airportdb -e "SELECT COUNT(*) AS flights FROM flights;"
docker exec -it mysql mysql -uroot -proot -D airportdb -e "SELECT COUNT(*) AS bookings FROM bookings;"

## ================================================================================================

## 6) “Лампочка” на весь стек одной командой

### Что демонстрируем:

Быстрый статус всех доменных сервисов через Gateway — сразу видно, что всё UP.

for s in pilot flight bookings security checkin boarding; do
echo "=== $s ==="
curl -s http://localhost:2021/airport-$s-service/actuator/health | jq -r '.status'
done

## ================================================================================================

## 7) Логи Gateway: как он подхватывает сервисы

### Что демонстрируем:

На старте могли быть предупреждения “No servers available…” (сервисы ещё не успели зарегистрироваться),
но потом всё стабилизируется — это нормальная фаза инициализации.

docker compose logs -n 200 api-gateway | \
grep -E "No servers available|RouteDefinition|LoadBalancer" || true

