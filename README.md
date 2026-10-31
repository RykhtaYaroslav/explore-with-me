# Explore With Me

Микросервисное приложение-афиша: пользователи публикуют события, подают заявки на участие, собирают события в подборки и оставляют оценки.

## Стек

- Java 21, Maven, Spring Boot 3.3, Spring Cloud.
- Spring Web, Spring Data JPA, Hibernate, OpenFeign, Spring Cloud Gateway, Eureka и Config Server.
- PostgreSQL для локального запуска; H2 для тестовых профилей.
- Docker Compose для запуска сервисов и баз данных.
- JUnit, Mockito и Postman для проверок; MapStruct и Lombok в коде.

## Архитектура

Внешние запросы поступают на API Gateway (`http://localhost:8080`). Gateway находит сервисы через Eureka и распределяет запросы по маршрутам. Config Server раздаёт сервисам настройки из файлового репозитория `infra/config-repo`. Бизнес-сервисы общаются по HTTP через OpenFeign и сохраняют данные в отдельных базах PostgreSQL.

| Сервис | Ответственность | Порт в Docker |
| --- | --- | ---: |
| `gateway-server` | Единая точка входа и маршрутизация внешнего API | 8080 |
| `discovery-server` | Регистрация и обнаружение сервисов (Eureka) | 8761 |
| `config-server` | Централизованная конфигурация | 8888 |
| `event-service` | События, категории и внутренняя выдача данных о событиях | Случайный порт (`server.port: 0`) |
| `user-service` | Пользователи и внутренние краткие сведения о них | Случайный порт (`server.port: 0`) |
| `review-service` | Оценки событий и авторов | Случайный порт (`server.port: 0`) |
| `participation-service` | Заявки на участие и проверка подтверждённого участия | 8083 |
| `compilation-service` | Подборки событий | Случайный порт (`server.port: 0`) |
| `stats-server` | Приём просмотров и выдача статистики | Случайный порт (`server.port: 0`) |

Для каждого бизнес-сервиса и `stats-server` в Compose настроена своя база данных. Сервис событий отправляет просмотры и запрашивает статистику через библиотеку `stats-client`. Сервисы событий и подборок собирают данные из других сервисов, а сервис оценок проверяет участников и авторов через внутренние API. Внешние маршруты Gateway и discovery-based адресация заданы в `infra/config-repo/gateway-server/gateway-server.yaml`.

### Где менять конфигурацию

- `docker-compose.yml` — контейнеры, порты, базы, переменные окружения, зависимости и health checks.
- `infra/config-server/src/main/resources/application.yml` — режим Config Server и адрес конфигурационного репозитория.
- `infra/config-repo/application.yml` — общие настройки.
- `infra/config-repo/<service>/<service>.yaml` — настройки конкретного сервиса; `*-test.yaml` — тестовый профиль.
- `*/src/main/resources/application.yaml` — имя приложения, начальные настройки подключения к Config Server/Eureka и профиль, если он задан локально.

Параметры подключения к базам можно переопределить переменными окружения, например `EVENT_DB_URL`, `EVENT_DB_USERNAME`, `EVENT_DB_PASSWORD` (аналогично для `USER`, `REVIEW`, `PARTICIPATION`, `COMPILATION`, `STATS`). Значения по умолчанию для локального Compose указаны в `docker-compose.yml` и конфигурации сервисов.

## API

### Внешний API

Локальная OpenAPI-спецификация проекта: [`openapi/explore-with-me.yaml`](openapi/explore-with-me.yaml). Она описывает API через Gateway на `http://localhost:8080`, включая маршруты событий, пользователей, заявок, оценок, подборок и статистики.

Исходные спецификации задания: [основной API](https://github.com/yandex-praktikum/java-explore-with-me/blob/main/ewm-main-service-spec.json) и [API статистики](https://github.com/yandex-praktikum/java-explore-with-me/blob/main/ewm-stats-service-spec.json).

Коллекции Postman лежат в [`postman/`](postman/): отдельные сценарии для пользовательского API, событий, заявок, оценок, подборок и статистики.

### Внутренний API сервисов

Внутренние REST-маршруты используются сервисами друг для друга. Клиенты Feign находятся в пакетах `interservice/feign` (или `client`); серверная сторона — в контроллерах `*InternalApiController`.

| Владелец | Маршруты | Для чего вызывают |
| --- | --- | --- |
| `user-service` | `GET /interservice/users/exist/{id}`, `GET /interservice/users/short/{id}`, `GET /interservice/users/short?ids=...` | Проверка пользователя и получение кратких данных для событий, заявок и оценок |
| `event-service` | `GET /interservice/events/exist/{eventId}`, `GET /interservice/events/{eventId}/organizers/{userId}`, `GET /interservice/events/{eventId}/review-info`, `GET /interservice/events/review-info?ids=...`, `GET /interservice/events/compilation-info?ids=...`, `GET /interservice/events/{eventId}/participation-info` | Проверка событий и организаторов, получение данных для оценок, подборок и заявок |
| `event-service` | `PUT /interservice/events/{eventId}/confirmed-requests?delta=...` или `?expectedCount=...&count=...` | Обновление счётчика подтверждённых заявок |
| `participation-service` | `GET /interservice/requests/users/{userId}/events/{eventId}/confirmed` | Проверка, что пользователь — участник с подтверждённой заявкой |
| `review-service` | `POST /interservice/reviews/events`, `POST /interservice/reviews/users` | Получение агрегированных оценок событий и авторов по списку ID в JSON body |
| `stats-server` | `POST /hit`, `GET /stats?start=...&end=...&uris=...&unique=...` | Регистрация просмотра и получение статистики; клиентский контракт находится в модуле `stats/stats-client` |

Внутренние маршруты не являются публичным контрактом для клиентских приложений. Схемы запросов и ответов описаны рядом с Feign-клиентами и контроллерами в исходном коде.

## Запуск

Нужны JDK 21, Maven и Docker с Compose.

```bash
docker compose up --build
```

После готовности сервисов API доступен на `http://localhost:8080`, Eureka — на `http://localhost:8761`. Состояние сервисов можно проверить через Actuator, например `http://localhost:8080/actuator/health` для Gateway. Остановить контейнеры можно командой `docker compose down`; чтобы удалить локальные данные баз, используйте `docker compose down -v`.

**Примечание по текущей конфигурации:** `review-service` загружает профиль `test-time` из `core/review-service/src/main/resources/application.yaml`; конфигурация этого профиля задаёт `server.port: 0` и H2. У `event-service`, `user-service`, `compilation-service` и `stats-server` также настроен случайный порт (`server.port: 0`), тогда как health checks в `docker-compose.yml` проверяют фиксированные порты. Исключение — `participation-service`, для которого Compose задаёт `SERVER_PORT=8083`. Перед локальным запуском через Compose динамические порты и health checks нужно согласовать, иначе зависимые сервисы, включая Gateway, могут не перейти в состояние `healthy`.

Собрать Maven-проект без запуска тестов:

```bash
mvn -DskipTests package
```

## Время в тестах Postman

Проверки оценок зависят от времени: оценку события можно оставить только после его окончания. В Postman-коллекции `ewm-review-service.postman_collection.json` pre-request-скрипты создают событие и перед отправкой оценки вызывают `PUT /test/time` через Gateway, устанавливая часам `review-service` подходящий момент относительно `eventDate` (обычно дата события плюс несколько часов; в сценариях до события задаётся время раньше него). Поэтому набор не ждёт реального окончания события и результат не зависит от системных часов.

Маршрут `/test/time` включён только для профиля Spring `test-time`; `GET /test/time` возвращает установленное значение, `PUT /test/time` принимает JSON-строку `LocalDateTime`, например `"2026-11-01T18:30:00"`. Часы подменяются только внутри `review-service`, общий `TimeProvider` читает их при проверке возможности оставить оценку. В этой версии профиль `test-time` активирован в `core/review-service/src/main/resources/application.yaml`; перед запуском интеграционных сценариев убедитесь, что маршрут доступен через Gateway. Сам endpoint предназначен для тестового окружения.

## Структура репозитория

- `core/` — бизнес-сервисы, общий код и контракты межсервисных вызовов.
- `stats/` — сервер статистики и клиентская библиотека.
- `infra/` — Gateway, Config Server, Eureka и файлы конфигурации.
- `api-contracts/` — общие DTO/контракты.
- `openapi/` — OpenAPI-спецификация внешнего API.
- `postman/` — коллекции ручных API-сценариев.
