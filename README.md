# TeaPack KPI Monitoring System
Арзиманов Г. К. \
ИАС для мониторинга и расчёта KPI участка фасовки и упаковки чайной продукции. ВКР, тема: «Информационно-аналитическая система мониторинга и расчёта эффективности производственного участка фасовки и упаковки чайной продукции».

## Стек

- **Backend**: Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.1, JPA/Hibernate, PostgreSQL 16, Flyway, MapStruct, Lombok, JJWT, springdoc-openapi.
- **Frontend**: React 19 + Vite, react-router-dom v7, Ant Design v6, Axios, Recharts, STOMP.js + SockJS.
- **Инфра**: Docker Compose, один PostgreSQL с раздельными схемами.

## Структура проекта

```
teapack-kpi-system/
├── api-gateway/              :8080  Spring Cloud Gateway
├── user-management-service/  :8087  auth, JWT, роли
├── equipment-emulator/       :8081  эмуляция данных оборудования
├── data-collection-service/  :8082  приём событий
├── data-processing-service/  :8083  Shift, ShiftAggregate, DowntimeEvent
├── kpi-calculation-service/  :8084  расчёт OEE, WebSocket /topic/kpi/{lineId}
├── reporting-service/        :8085  отчёты по сменам
├── notification-service/     :8086  уведомления об отклонениях KPI
├── frontend/                 :5173  React UI
├── docker-compose.yml
├── init-db.sql               схемы БД
└── pom.xml                   родительский Maven POM
```

## БД

Единая БД `teapack_kpi_db`, схемы по сервисам: `users`, `collection`, `processing`, `kpi`, `reporting`, `notification`. Миграции — Flyway, лежат в `src/main/resources/db/migration/` каждого сервиса.

## Доменная модель смены

Сущность `Shift` — в `data-processing-service`, таблица `processing.shifts`.

Поля: `id`, `lineId`, `operatorId`, `plannedStart`, `plannedEnd`, `actualStart`, `actualEnd`, `plannedOutput`, `nominalSpeed`, `status`, `createdAt`.

Статусы: `PLANNED` → `ACTIVE` → `CLOSED`. Также `CANCELLED` (мягкая отмена из `PLANNED` через `DELETE /api/shifts/{id}`). Создаётся в БД сразу при создании в статусе `PLANNED` (см. `Shift.prePersist`).

Связанные сущности:
- `ShiftAggregate` (totalOutput, goodOutput, scrapCount, downtimeMinutes, avgSpeed)
- `DowntimeEvent` (события простоев)
- `ShiftKpi` в `kpi-calculation-service` (OEE, availability, performance, quality)

## Справочник производственных линий

Таблица `processing.production_lines`, сущность `ProductionLine`. Поля включают пороги для outlier detection: `min_speed`, `max_speed`, `min_temperature`, `max_temperature`. Также `nominal_speed` и `planned_output_per_hour` — используются для автозаполнения формы создания смены на фронте.

## Outlier detection (ТЗ 2.2)

Реализовано в `data-collection-service`:
- При приёме показания `ValidationService` проверяет диапазоны скорости/температуры по справочнику линий (с TTL-кэшем 60 сек, fetch через Feign-клиент `LinesClient`).
- При выходе значений за допустимые пороги показание сохраняется с `is_valid=false` и текстом `validation_note`, но **не пересылается** в `data-processing-service` (не попадает в агрегаты/KPI).
- Аудит выбросов — `GET /api/collect/readings/invalid?lineId=&limit=`.

## Ролевой доступ на фронте

- `ROLE_ADMIN` — все разделы.
- `ROLE_OPERATOR` — `/operator`, `/dashboard`, `/shifts/planned`.
- `ROLE_TECHNOLOGIST` — `/dashboard`, `/shifts/planned`, `/shifts/history`, `/reports`.

Маппинг — `frontend/src/constants/access.js`. Меню фильтруется в `MainLayout`, маршруты обернуты в `RoleGuard` (`App.jsx`).

## Ключевые endpoint'ы

Все запросы с фронта идут через `api-gateway` (`http://localhost:8080`).

| Метод | Путь | Сервис | Назначение |
|------|------|--------|-----------|
| POST | `/api/auth/login` | user-management | вход, выдача JWT |
| POST | `/api/auth/register` | user-management | регистрация |
| POST | `/api/shifts` | data-processing | создать смену (PLANNED) |
| POST | `/api/shifts/{id}/start` | data-processing | запустить (ACTIVE) |
| POST | `/api/shifts/{id}/close` | data-processing | завершить (CLOSED) |
| GET  | `/api/shifts/{id}` | data-processing | получить смену |
| GET  | `/api/shifts/{id}/data` | data-processing | смена + агрегаты + downtime |
| GET  | `/api/shifts` | data-processing | список смен с фильтрами (status/lineId/operatorId/dateFrom/dateTo) |
| GET  | `/api/shifts/line/{lineId}` | data-processing | смены по линии |
| DELETE | `/api/shifts/{id}` | data-processing | мягкая отмена PLANNED-смены (status=CANCELLED) |
| GET  | `/api/kpi/shift/{shiftId}` | kpi-calculation | KPI смены |
| GET  | `/api/kpi/line/{lineId}` | kpi-calculation | список KPI по линии |
| GET  | `/api/kpi/history` | kpi-calculation | история CLOSED смен с фильтрами по периоду и порогам KPI |
| GET  | `/api/lines` | data-processing | справочник производственных линий (`activeOnly`) |
| GET  | `/api/lines/{code}` | data-processing | линия по коду |
| POST/PUT/DELETE | `/api/lines[/{id}]` | data-processing | CRUD линий (admin) |
| GET  | `/api/reports/line/{lineId}` | reporting | отчёты по линии |
| POST | `/api/collect/operator-event` | data-collection | события оператора (STOP/START/SCRAP) |
| GET  | `/api/collect/readings/invalid` | data-collection | помеченные как outlier показания (admin/audit) |
| POST | `/api/emulator/start` \| `/stop` | equipment-emulator | управление эмулятором |
| GET  | `/api/notifications/unread` | notification | непрочитанные уведомления |

## Фронтенд

- Точка входа: `frontend/src/main.jsx`, главный роутер: `frontend/src/App.jsx`.
- Layout с боковым меню: `frontend/src/components/layout/MainLayout.jsx`.
- API-обёртки: `frontend/src/api/*.js` (один axios instance в `client.js` с Bearer-токеном и обработкой 401).
- Страницы: `pages/login`, `pages/operator`, `pages/dashboard`, `pages/reports`.
- Хуки: `hooks/useWebSocket.js` для real-time KPI.
- Auth: токен/user/role хранятся в localStorage (см. `utils/auth.js`).

## Запуск

```bash
docker compose up --build
cd frontend && npm install && npm run dev
```

Дефолтные креды БД: `teapack` / `teapack123`.

## KPI набор (по ТЗ)

Базовые (4): Availability, Performance, Quality, OEE.
Время (6): PlannedTime, OperatingTime, Downtime, DowntimeRate, NumberOfStops, AvgDowntimeDuration.
Производительность (4): OutputRate, SpeedLoss, PerformanceLoss, PlanFulfillment.
Качество (1): ScrapRate.

## Соглашения

- **Java**: Lombok (`@Getter`/`@Setter`/`@RequiredArgsConstructor`/`@Builder`), MapStruct для маппинга, JPA-сущности под `@Entity` + `@Table(schema=...)`.
- **DTO**: отдельные DTO в `dto/`, контроллеры возвращают DTO или сущность через `ResponseEntity`.
- **Frontend**: Ant Design компоненты, формы через `Form.useForm()`, таблицы AntD `Table`.
- **Локализация**: UI на русском, имена endpoint'ов и сущностей — на английском.
- **Комментарии**: код самодокументируемый, комментарии — только если объясняют «почему».

