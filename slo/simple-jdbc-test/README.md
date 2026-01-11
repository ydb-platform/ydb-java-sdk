# simple-jdbc-test

Модуль, содержащий полноценный SLO тест с использованием JDBC подключения к YDB.

## Компоненты

### JdbcSloTableContext
Сервисный класс для работы с таблицей `slo_table`. Реализует retry logic с exponential backoff.

**Методы:**
- `createTable(timeout)` - создание таблицы с составным ключом (Guid, Id)
- `save(row, timeout)` - UPSERT запись с автоматическим retry (до 5 попыток)
- `select(guid, id, timeout)` - чтение записи по ключу с retry
- `selectCount()` - подсчёт общего количества записей
- `tableExists()` - проверка существования таблицы
- `isRetryableError(exception)` - определение временных ошибок (timeout, network, overload)

**Retry стратегия:**
- Максимум попыток: 5
- Backoff: 100ms → 200ms → 400ms → 800ms → 1600ms
- Повторяет: timeout, connection, network, overload, session expired
- Не повторяет: schema errors, constraint violations, syntax errors

### SloTableRow
Data Transfer Object для строки таблицы. Используется для тестирования и будущих workload'ов.

**Поля:**
- `guid: UUID` - уникальный идентификатор
- `id: int` - порядковый номер
- `payloadStr: String` - строковая нагрузка (~1KB)
- `payloadDouble: double` - числовая нагрузка
- `payloadTimestamp: Timestamp` - время создания

**Методы:**
- `generate(id)` - генерация случайной строки с заданным id
- `generatePayloadString(size)` - создание payload заданного размера

### JdbcSloTest
Основной SLO тест. Выполняет полный цикл нагрузочного тестирования.

**Фазы выполнения:**
1. **Table Initialization** - создание таблицы
2. **Data Preparation** - генерация и запись начальных данных
3. **SLO Test Execution** - параллельная нагрузка (read + write) в течение заданного времени
4. **Results Validation** - проверка соответствия SLO порогам
5. **Metrics Export** - отправка метрик в Prometheus и сохранение в файл

**SLO пороги:**
- P50 Latency: < 10ms
- P95 Latency: < 50ms
- P99 Latency: < 100ms
- Success Rate: > 99.9%

**Параметры окружения:**
- `TEST_DURATION` - длительность теста в секундах (default: 60)
- `READ_RPS` - read операций в секунду (default: 100)
- `WRITE_RPS` - write операций в секунду (default: 10)
- `READ_TIMEOUT` - timeout для read в ms (default: 1000)
- `WRITE_TIMEOUT` - timeout для write в ms (default: 1000)
- `PROM_PGW` - URL Prometheus Push Gateway (default: http://localhost:9091)
- `REPORT_PERIOD` - период отправки метрик в ms (default: 10000)
- `YDB_JDBC_URL` - строка подключения к YDB

### MetricsReporter
Класс для сбора и отправки метрик в Prometheus Push Gateway.

**Метрики:**
- `jdbc_test_success_total` - Counter успешных операций (labels: operation)
- `jdbc_test_errors_total` - Counter ошибок (labels: operation, error_type)
- `jdbc_test_latency_seconds` - Histogram latency (labels: operation)
- `jdbc_test_active_connections` - Gauge активных подключений

**Методы:**
- `recordSuccess(operation, latency)` - запись успешной операции
- `recordError(operation, errorType)` - запись ошибки
- `push()` - отправка метрик в Prometheus (полная замена)
- `pushAdd()` - инкрементальное обновление метрик
- `saveToFile(filename, latency)` - сохранение в файл для GitHub Summary

### SimpleJdbcConfig
Spring конфигурация для DataSource и JdbcTemplate.

**Beans:**
- `dataSource()` - DriverManagerDataSource для YDB JDBC Driver
- `jdbcTemplate(dataSource)` - Spring JdbcTemplate (для будущего использования)