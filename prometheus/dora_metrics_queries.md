# PromQL запросы для DevOps/SRE метрик

В этом файле собраны готовые PromQL запросы для мониторинга ключевых DevOps/SRE метрик, включая DORA метрики, в Prometheus и Grafana.

## DORA метрики

### Deployment Frequency

```promql
# Количество деплоев в неделю
sum(increase(deploy_count{environment="production", status="success"}[1w]))

# Количество деплоев в день (средний показатель за неделю)
sum(increase(deploy_count{environment="production", status="success"}[1w])) / 7

# Количество деплоев по командам
sum(increase(deploy_count{environment="production", status="success"}[1w])) by (team)

# Тренд деплоев (еженедельный)
sum(rate(deploy_count{environment="production", status="success"}[1d]) * 86400 * 7)
```

### Lead Time for Changes

```promql
# Среднее время от коммита до деплоя (в часах)
avg(lead_time_seconds{environment="production"}) / 3600

# Медианное время от коммита до деплоя (в часах)
histogram_quantile(0.5, sum(rate(lead_time_bucket{environment="production"}[1w])) by (le)) / 3600

# 95-й процентиль времени от коммита до деплоя (в часах)
histogram_quantile(0.95, sum(rate(lead_time_bucket{environment="production"}[1w])) by (le)) / 3600

# Среднее время по командам (в часах)
avg(lead_time_seconds{environment="production"}) by (team) / 3600
```

### Change Failure Rate

```promql
# Общий процент неудачных деплоев
sum(deploy_incidents_count{environment="production"}) / sum(deploy_count{environment="production"}) * 100

# Процент неудачных деплоев по командам
sum(deploy_incidents_count{environment="production"}) by (team) / sum(deploy_count{environment="production"}) by (team) * 100

# Динамика Change Failure Rate за последние 30 дней (по неделям)
sum(increase(deploy_incidents_count{environment="production"}[7d])) / sum(increase(deploy_count{environment="production"}[7d])) * 100
```

### Mean Time to Recovery (MTTR)

```promql
# Среднее время восстановления (в минутах)
avg(mttr_seconds{environment="production"}) / 60

# Медианное время восстановления (в минутах)
histogram_quantile(0.5, sum(rate(mttr_bucket{environment="production"}[30d])) by (le)) / 60

# MTTR по командам (в минутах)
avg(mttr_seconds{environment="production"}) by (team) / 60

# MTTR по типам инцидентов (в минутах)
avg(mttr_seconds{environment="production"}) by (incident_type) / 60
```

## Метрики SRE

### SLO/SLI

```promql
# Доступность сервиса (success rate) за последний час
sum(rate(http_requests_total{status=~"2.."}[1h])) / sum(rate(http_requests_total[1h])) * 100

# Error Budget (если SLO = 99.9%)
100 - (sum(rate(http_requests_total{status=~"2.."}[30d])) / sum(rate(http_requests_total[30d])) * 100) / (100 - 99.9) * 100

# Латентность API (95-й процентиль, мс)
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)) * 1000

# Соответствие SLO по сервисам
avg_over_time(slo_status{slo="availability"}[1d]) * 100
```

### Метрики производительности

```promql
# CPU Usage
100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory Usage
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# Disk Usage
(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100

# Network I/O
sum(rate(node_network_receive_bytes_total[5m])) + sum(rate(node_network_transmit_bytes_total[5m]))
```

### Метрики инцидентов

```promql
# Количество инцидентов по сервисам за 30 дней
sum(increase(incident_count[30d])) by (service)

# MTTD (Mean Time to Detect) в минутах
avg(mttd_seconds) / 60

# MTTA (Mean Time to Acknowledge) в минутах
avg(mtta_seconds) / 60

# MTBF (Mean Time Between Failures) в днях
avg(mtbf_seconds) / 86400
```

### Метрики CI/CD

```promql
# Процент успешных сборок
sum(increase(build_count{status="success"}[1d])) / sum(increase(build_count[1d])) * 100

# Длительность CI/CD пайплайна (среднее, секунды)
avg(pipeline_duration_seconds)

# Длительность CI/CD пайплайна (медиана, секунды)
histogram_quantile(0.5, sum(rate(pipeline_duration_bucket[1d])) by (le))

# Время, затраченное на проверку кода (Code Review Time, в часах)
avg(code_review_time_seconds) / 3600
```

### Метрики TeamCity

```promql
# Количество запущенных сборок
sum(teamcity_builds_started_count)

# Время выполнения сборок (среднее, секунды)
avg(teamcity_build_duration_seconds)

# Время ожидания в очереди (среднее, секунды)
avg(teamcity_build_queue_duration_seconds)

# Количество агентов в работе
sum(teamcity_agent_status{status="running"})
```

## Метрики для Grafana Alerts

### Алерты по DORA метрикам

```promql
# Алерт: Lead Time сильно вырос
avg(lead_time_seconds) / 3600 > 48

# Алерт: Change Failure Rate превысил допустимый порог
sum(deploy_incidents_count) / sum(deploy_count) * 100 > 20

# Алерт: MTTR превысил SLO
avg(mttr_seconds) / 60 > 120
```

### Алерты по SRE метрикам

```promql
# Алерт: SLO не выполняется
sum(rate(http_requests_total{status=~"2.."}[5m])) / sum(rate(http_requests_total[5m])) * 100 < 99.9

# Алерт: Error Budget израсходован более чем на 80%
100 - (sum(rate(http_requests_total{status=~"2.."}[30d])) / sum(rate(http_requests_total[30d])) * 100) / (100 - 99.9) * 100 > 80

# Алерт: Высокая латентность API
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)) * 1000 > 500
```

## Комбинированные метрики

```promql
# Качество разработки (составной индекс из нескольких метрик)
(
  (sum(increase(deploy_count{environment="production", status="success"}[30d])) / 30) * 0.25 +
  (100 - (sum(deploy_incidents_count{environment="production"}) / sum(deploy_count{environment="production"}) * 100)) * 0.25 +
  (100 - min(avg(lead_time_seconds) / 3600 / 24, 30) / 30 * 100) * 0.25 +
  (100 - min(avg(mttr_seconds) / 60 / 60, 24) / 24 * 100) * 0.25
) / 100 * 100

# Индекс DevOps-зрелости (на основе DORA)
(
  clamp_max(sum(increase(deploy_count{environment="production", status="success"}[7d])) / 7, 1) * 25 +
  clamp_max((24 - min(avg(lead_time_seconds) / 3600, 24)) / 24, 1) * 25 +
  clamp_max((30 - min(sum(deploy_incidents_count{environment="production"}) / sum(deploy_count{environment="production"}) * 100, 30)) / 30, 1) * 25 +
  clamp_max((60 - min(avg(mttr_seconds) / 60, 60)) / 60, 1) * 25
)
```

## Полезные функции Prometheus

- `rate()` - скорость изменения метрики по времени
- `increase()` - прирост метрики за период
- `avg_over_time()` - среднее значение за период
- `histogram_quantile()` - вычисление процентиля для гистограмм
- `sum()` - суммирование метрик
- `avg()` - среднее значение метрик
- `min()` / `max()` - минимальное/максимальное значение
- `clamp_max()` / `clamp_min()` - ограничение значения сверху/снизу

Эти запросы можно использовать для создания информативных дашбордов в Grafana, отслеживания метрик в реальном времени и настройки алертов. 