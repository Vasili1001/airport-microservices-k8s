# Kubernetes

## Работа пошагово

Собрали Docker-образы всех сервисов:
eureka-server, api-gateway, и 6 доменных сервисов (pilot/flight/bookings/security/checkin/boarding).

Положили образы в кэш Minikube:
minikube image load airport-microservices-<service>:latest — чтобы kube-узел не тянул их из внешнего реестра.

Сгенерировали манифесты Kubernetes из docker-compose.yml:
kompose convert -f docker-compose.yml -o k8s/
Получили Deployment, Service, ConfigMap, PVC (для MySQL).

Создали namespace и применили манифесты:
kubectl create ns airport
kubectl apply -n airport -f k8s/

Исправили нюансы:
Проставили корректные image (с :latest) и imagePullPolicy: IfNotPresent в деплойментах (через kubectl patch), чтобы брать локальные образы.
Сделали kubectl rollout restart и дождались раскатки kubectl rollout status.

Убедились, что всё работает:
kubectl -n airport get deploy,pods → у всех сервисов READY 1/1 и STATUS Running.

Открыли доступ:
Вариант для демо — port-forward: kubectl -n airport port-forward svc/eureka-server 8761:8761 и kubectl -n airport port-forward svc/api-gateway 2021:2021.

## Что сейчас работает

Все 9 приложений идут в Kubernetes в namespace=airport:
Deployments: eureka-server, api-gateway, 6 бизнес-сервисов, mysql.
Services: ClusterIP (внутри кластера) для каждого приложения.
MySQL с PersistentVolumeClaim.

Сетевое взаимодействие:
Сервисы регистрируются в Eureka.
API Gateway маршрутизирует запросы к нужным сервисам.

Состояние подтверждается:
kubectl -n airport get pods -o wide → все Running.
kubectl -n airport get deploy → AVAILABLE = 1 у каждой.

## Как запустить (последовательно)

Если кластер уже включён и манифесты применены — у тебя всё уже запущено. Если нужно «с нуля»:

1. minikube start
2. (один раз) kubectl create ns airport
3. minikube image load airport-microservices-<service>:latest для всех 8 образов
4. kubectl apply -n airport -f k8s/
5. (при необходимости) поправить image/imagePullPolicy и kubectl rollout restart
6. Проверка:
7. kubectl -n airport get deploy,pods -o wide

## Как остановить

Остановить весь кластер (самый простой способ):
minikube stop

Удалить только ресурсы приложения (кластер оставить):
kubectl delete -n airport -f k8s/

Временно “приглушить” сервисы (оставить манифесты):
kubectl -n airport scale deploy --all --replicas=0

## Использование Kubernetes

### 1) Показать ресурсы k8s приложения

   kubectl get ns
   kubectl -n airport get deploy,svc,cm,pvc
   kubectl -n airport get pods -o wide

Демонстрируем: приложение развернуто в Kubernetes (namespace, Deployments, Services, PVC), всё в статусе Running.
Зачем: эксплуатационная управляемость и оркестрация.

### 2) Логи, события, раскатка

   kubectl -n airport logs deploy/eureka-server --tail=50
   kubectl -n airport rollout status deploy/api-gateway
   kubectl -n airport get events --sort-by=.lastTimestamp | tail -n 20

Демонстрируем: нативные возможности Kubernetes: логирование, rollout/rollback, события.

### 3) Сервис-дискавери (Eureka)

(предварительно сделать port-forward в двух отдельных терминалах)
kubectl -n airport port-forward svc/eureka-server 8761:8761 
kubectl -n airport port-forward svc/api-gateway  2021:2021

EUREKA=http://localhost:8761
GATEWAY=http://localhost:2021
curl -s -H 'Accept: application/json' "$EUREKA/eureka/apps" | jq '.applications.application[].name'

Демонстрируем: все микросервисы зарегистрированы в Eureka и видны реестру.

### 4) Маршрутизация через API Gateway

curl -s "$GATEWAY/actuator/gateway/routes" | jq '.[].route_id'

Демонстрируем: gateway знает маршруты, интегрирован с Eureka, единая точка входа.

### 5) Health каждого сервиса (сквозной путь)

Демонстрируем: живучесть всех сервисов через gateway → значит, discovery + маршрутизация работают.

### 6) Прикладные эндпоинты (бизнес-логика)

curl -s "$GATEWAY/airport-flight-service/flights" | jq
curl -s "$GATEWAY/airport-bookings-service/bookings" | jq
curl -s "$GATEWAY/airport-checkin-service/checkins" | jq

Демонстрируем: функция «для пассажиров и аэропорта» реально отвечает, данные идут через gateway → сервисы взаимодействуют.

## Что делает каждый сервис

1. Eureka Server — реестр сервисов (кто где живёт). Нужен для динамического service discovery.
2. API Gateway — единая точка входа, маршрутизация запросов на нужный бэкенд, кросс-срезовые политики.
3. airport-flight-service — рейсы (основа для пассажиров).
4. airport-bookings-service — бронирования (связь с рейсами и пользователями).
5. airport-checkin-service — регистрация на рейс.
6. airport-security-service — проверки безопасности/статусы.
7. airport-boarding-service — посадка.
8. airport-pilot-service — информация/операции, связанные с экипажем/пилотами (в твоей доменной модели). 
9. MySQL — база данных с PVC.

## Зачем нам Kubernetes и чем он лучше

1. Оркестрация и самовосстановление: перезапуск упавших контейнеров, поддержание заявленного числа реплик.
2. Масштабирование: kubectl scale/HPA — легко увеличивать/уменьшать реплики под нагрузку.
3. Сетевые примитивы: Service/Endpoints/Ingress обеспечивают стабильные адреса и балансировку внутри кластера.
4. Декларативность: всё описано в манифестах — воспроизводимо, прозрачно, версионируемо.
5. Наблюдаемость и операции: события, логи, rollout/rollback, probes (liveness/readiness).
6. Изоляция и безопасность: namespaces, политики, quotas.
7. Портируемость: локально (minikube), в облаке (GKE/EKS/AKS) — одни и те же манифесты.

# ======================================================================================================================

NS=airport
EUREKA=http://localhost:8761
GATEWAY=http://localhost:2021

## Состояние кластера и неймспейса

minikube status
kubectl get ns
kubectl -n $NS get deploy,svc,cm,pvc
kubectl -n $NS get pods -o wide
kubectl -n $NS get events --sort-by=.lastTimestamp | tail -n 50

## Проброс портов (2 вкладки терминала)
Eureka: kubectl port-forward -n $NS svc/eureka-server 8761:8761
API Gateway: kubectl port-forward -n $NS svc/api-gateway  2021:2021

## Проверка Eureka и Gateway

# список зарегистрированных приложений в Eureka
curl -s -H 'Accept: application/json' "$EUREKA/eureka/apps" | jq '.applications.application[].name'

# маршруты, которые построил gateway (discovery locator)
curl -s "$GATEWAY/actuator/gateway/routes" | jq '.[].route_id'

# сам health gateway и eureka
curl -s "$GATEWAY/actuator/health" | jq
curl -s "$EUREKA/actuator/health" | jq

## Health всех микросервисов (через gateway)

for s in airport-pilot-service airport-flight-service airport-bookings-service \
airport-security-service airport-checkin-service airport-boarding-service
do
echo "=== $s ==="
curl -s "$GATEWAY/$s/actuator/health" | jq
done




# что зарегистрировано в eureka (есть все сервисы)
curl -s "$GATEWAY/actuator/health" | jq '.components.discoveryComposite.components.eureka.details.applications'

# health каждого сервиса через gateway
for p in pilot flight bookings security checkin boarding; do
echo "=== $p ==="; curl -s "$GATEWAY/$p/actuator/health" | jq; echo;
done


