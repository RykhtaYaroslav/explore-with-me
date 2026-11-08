# Explore With Me

Микросервисное приложение-афиша. Пользователи публикуют события, подают заявки на участие, собирают события в подборки, оставляют оценки и получают персональные рекомендации.

## Стек

- Java 21, Maven, Spring Boot 3.3, Spring Cloud.
- Spring Web, Spring Data JPA, Hibernate, OpenFeign, Spring Cloud Gateway, Eureka и Config Server.
- gRPC и Protobuf для взаимодействия с системой рекомендаций, Kafka и Avro для обработки действий.
- PostgreSQL для локального запуска; H2 для тестовых профилей.
- Docker Compose для запуска сервисов и баз данных.
- JUnit, Mockito и Postman для проверок; MapStruct и Lombok в коде.

## Архитектура

Внешние запросы поступают на API Gateway (`http://localhost:8080`). Gateway находит сервисы через Eureka и распределяет запросы по маршрутам. Config Server раздаёт сервисам настройки из файлового репозитория `infra/config-repo`. Бизнес-сервисы общаются по HTTP через OpenFeign и сохраняют данные в отдельных базах PostgreSQL.

| Сервис | Ответственность | Порт в Docker |
| --- | --- | ---: |
| `gateway-server` | Единая точка входа и маршрутизация внешнего API | 8080 |
| `discovery-server` | Регистрация и обнаружение сервисов (Eureka) | 8761 |
| `config-server` | Централизованная конфигурация | Случайный порт, регистрация в Eureka |
| `event-service` | События, категории и внутренняя выдача данных о событиях | Случайный порт, регистрация в Eureka |
| `user-service` | Пользователи и внутренние краткие сведения о них | Случайный порт, регистрация в Eureka |
| `review-service` | Оценки событий и авторов | Случайный порт, регистрация в Eureka |
| `participation-service` | Заявки на участие и проверка подтверждённого участия | Случайный порт, регистрация в Eureka |
| `compilation-service` | Подборки событий | Случайный порт, регистрация в Eureka |
| `collector` | Приём действий по gRPC и публикация в Kafka | Случайный порт gRPC, регистрация в Eureka |
| `aggregator` | Расчёт сходства событий по действиям из Kafka | Регистрация в Eureka |
| `analyzer` | Расчёт рекомендаций и выдача весов взаимодействий | Случайный порт gRPC, регистрация в Eureka |

Для бизнес-сервисов, Aggregator и Analyzer в Compose настроены отдельные базы данных. `event-service`, `participation-service` и `review-service` обращаются к Collector через `stats-client`. `event-service` также запрашивает веса и рекомендации у Analyzer. Клиенты находят gRPC-порты через Eureka по адресам `discovery:///collector` и `discovery:///analyzer`. Настройки клиентов включают keepalive и plaintext.

У экземпляров сервисов нет закреплённых имён контейнеров: можно поднять несколько копий сервиса, например `docker compose up --build --scale event-service=2`. Eureka регистрирует каждую копию с её фактическим портом, а Gateway и Feign выбирают доступные экземпляры по имени приложения.

### Где менять конфигурацию

- `docker-compose.yml` — контейнеры, порты, базы, переменные окружения, зависимости и health checks.
- `infra/config-server/src/main/resources/application.yml` — режим Config Server и адрес конфигурационного репозитория.
- `infra/config-repo/application.yml` — общие настройки.
- `infra/config-repo/<service>/<service>.yaml` — настройки конкретного сервиса; `*-test.yaml` — тестовый профиль.
- `*/src/main/resources/application.yaml` — имя приложения, начальные настройки подключения к Config Server/Eureka и профиль, если он задан локально.

Параметры подключения к базам можно переопределить переменными окружения, например `EVENT_DB_URL`, `EVENT_DB_USERNAME`, `EVENT_DB_PASSWORD` (аналогично для `USER`, `REVIEW`, `PARTICIPATION` и `COMPILATION`). Значения по умолчанию для локального Compose указаны в `docker-compose.yml` и конфигурации сервисов. Compose включает профиль `docker` для работы с PostgreSQL. Порты бизнес-сервисов и Config Server случайные (`server.port: 0`). Compose ждёт регистрации приложений в Eureka перед запуском Gateway.

## API

### Внешний API

Спецификация [`openapi/explore-with-me.yaml`](openapi/explore-with-me.yaml) описывает внешние маршруты Gateway на `http://localhost:8080`, включая события, рекомендации, пользователей, заявки, оценки и подборки.

- `GET /events/{id}` отправляет `VIEW`, если `X-EWM-USER-ID` содержит ID существующего пользователя. Analyzer учитывает один просмотр на пользователя для каждого события после обработки сообщения Kafka. Карточки получают значение `views` из Analyzer.
- `POST /users/{userId}/requests?eventId=…` отправляет `REGISTER` после фиксации заявки. Это относится к заявкам со статусами `PENDING` и `CONFIRMED`. Ошибка Collector журналируется, а сохранённая заявка остаётся доступной.
- Оценки события 4 и 5 отправляют `LIKE`. Поле `rating` отражает средний балл отзывов, а `totalInteractionWeight` — сумму максимальных весов действий пользователей с событием.
- `GET /events/recommendations` принимает обязательный `X-EWM-USER-ID`. Рекомендации и веса обновляются по мере обработки сообщений Kafka.

В каталоге [`postman/`](postman/) лежат сценарии для пользователей, событий, заявок, оценок и подборок.

### Внутренний API сервисов

Внутренние REST-маршруты используются сервисами друг для друга. Клиенты Feign находятся в пакетах `interservice/feign` (или `client`); серверная сторона — в контроллерах `*InternalApiController`.

| Владелец | Маршруты | Для чего вызывают |
| --- | --- | --- |
| `user-service` | `GET /interservice/users/exist/{id}`, `GET /interservice/users/short/{id}`, `GET /interservice/users/short?ids=...` | Проверка пользователя и получение кратких данных для событий, заявок и оценок |
| `event-service` | `GET /interservice/events/exist/{eventId}`, `GET /interservice/events/{eventId}/organizers/{userId}`, `GET /interservice/events/{eventId}/review-info`, `GET /interservice/events/review-info?ids=...`, `GET /interservice/events/compilation-info?ids=...`, `GET /interservice/events/{eventId}/participation-info` | Проверка событий и организаторов, получение данных для оценок, подборок и заявок |
| `event-service` | `PUT /interservice/events/{eventId}/confirmed-requests?delta=...` или `?expectedCount=...&count=...` | Обновление счётчика подтверждённых заявок |
| `participation-service` | `GET /interservice/requests/users/{userId}/events/{eventId}/confirmed` | Проверка, что пользователь — участник с подтверждённой заявкой |
| `review-service` | `POST /interservice/reviews/events`, `POST /interservice/reviews/users` | Получение агрегированных оценок событий и авторов по списку ID в JSON body |

Collector принимает `CollectUserAction`, Analyzer обслуживает `GetRecommendationsForUser`, `GetSimilarEvents`, `GetInteractionsCount` и `GetViewCounts`. Protobuf-схемы находятся в [`stats/recommendation-contracts/src/main/proto/`](stats/recommendation-contracts/src/main/proto/).

## Запуск

Нужны JDK 21, Maven и Docker с Compose.

```bash
docker compose up --build
```

После готовности сервисов API доступен на `http://localhost:8080`, Eureka — на `http://localhost:8761`. Порты баз данных публикуются на случайные локальные порты, доступные только с этой машины; узнать назначенный порт можно командой `docker compose port event-db 5432` (замените `event-db` на имя нужной базы). Остановить контейнеры можно командой `docker compose down`; чтобы удалить локальные данные баз, используйте `docker compose down -v`.

Для ручных тестов оценок с подменой времени запустите `review-service` с дополнительным профилем `test-time`: например, в PowerShell выполните `$env:REVIEW_PROFILES_ACTIVE='docker,test-time'` перед `docker compose up --build`. Этот профиль переключает базу оценок на H2 и включает `/test/time`; обычный профиль Compose `docker` использует PostgreSQL.

Собрать Maven-проект без запуска тестов:

```bash
mvn -DskipTests package
```

## Время в тестах Postman

Проверки оценок зависят от времени: оценку события можно оставить только после его окончания. В Postman-коллекции `ewm-review-service.postman_collection.json` pre-request-скрипты создают событие и перед отправкой оценки вызывают `PUT /test/time` через Gateway, устанавливая часам `review-service` подходящий момент относительно `eventDate` (обычно дата события плюс несколько часов; в сценариях до события задаётся время раньше него). Поэтому набор не ждёт реального окончания события и результат не зависит от системных часов.

Маршрут `/test/time` включён только для профиля Spring `test-time`; `GET /test/time` возвращает установленное значение, `PUT /test/time` принимает JSON-строку `LocalDateTime`, например `"2026-11-01T18:30:00"`. Часы подменяются только внутри `review-service`, общий `TimeProvider` читает их при проверке возможности оставить оценку. В этой версии профиль `test-time` активирован в `core/review-service/src/main/resources/application.yaml`; перед запуском интеграционных сценариев убедитесь, что маршрут доступен через Gateway. Сам endpoint предназначен для тестового окружения.

## Структура репозитория

- `core/` — бизнес-сервисы, общий код и контракты межсервисных вызовов.
- `stats/` — Collector, Aggregator, Analyzer, gRPC-клиенты и общие контракты рекомендаций.
- `infra/` — Gateway, Config Server, Eureka и файлы конфигурации.
- `api-contracts/` — общие DTO/контракты.
- `openapi/` — OpenAPI-спецификация внешнего API.
- `postman/` — коллекции ручных API-сценариев.
