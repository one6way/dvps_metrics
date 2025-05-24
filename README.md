# DevOps & SRE Metrics Guide

<div align="center">
  <img src="https://img.shields.io/badge/TeamCity-000000?style=for-the-badge&logo=teamcity&logoColor=white">
  <img src="https://img.shields.io/badge/Grafana-F2F4F9?style=for-the-badge&logo=grafana&logoColor=orange">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white">
</div>

## Полное оглавление
1. [Введение: зачем нужны метрики](#введение)
2. [DORA-метрики](#dora-метрики)
    - [Deployment Frequency](#deployment-frequency)
    - [Lead Time for Changes](#lead-time-for-changes)
    - [Change Failure Rate](#change-failure-rate)
    - [Mean Time to Recovery](#mean-time-to-recovery)
    - [Сбор DORA-метрик в TeamCity](#сбор-dora-метрик-в-teamcity)
3. [Ключевые метрики DevOps/SRE](#ключевые-метрики-devopssre)
    - [Доступность и SLA/SLO/SLI](#доступность-и-slaslosli)
    - [Метрики производительности](#метрики-производительности)
    - [Метрики инцидентов](#метрики-инцидентов)
    - [Метрики CI/CD Pipeline](#метрики-cicd-pipeline)
4. [Инструменты для сбора и визуализации](#инструменты-для-сбора-и-визуализации)
    - [TeamCity: настройка и сбор метрик](#teamcity-настройка-и-сбор-метрик)
    - [Grafana: дашборды и визуализация](#grafana-дашборды-и-визуализация)
    - [Prometheus: запросы и сбор данных](#prometheus-запросы-и-сбор-данных)
5. [Практические примеры](#практические-примеры)
    - [TeamCity: отслеживание сборок](#teamcity-отслеживание-сборок)
    - [Grafana: готовые дашборды](#grafana-готовые-дашборды)
    - [Prometheus: PromQL запросы](#prometheus-promql-запросы)
6. [Best practices внедрения метрик](#best-practices)
7. [Полезные ссылки и литература](#полезные-ссылки)

---

## Введение

### Зачем нужны метрики в DevOps/SRE

Метрики — это не просто набор чисел, а инструмент для принятия решений, основанных на данных. Они позволяют:

- **Измерять скорость и стабильность изменений** — отслеживать прогресс и эффективность команды
- **Находить узкие места** — выявлять проблемные зоны в процессах разработки и эксплуатации
- **Улучшать процессы на основе данных** — принимать решения на основе фактов, а не догадок
- **Повышать доверие бизнеса к IT** — демонстрировать ценность и эффективность DevOps-практик

### Категории метрик:

- **Метрики скорости** — насколько быстро доставляются изменения
- **Метрики стабильности** — насколько надежны изменения
- **Метрики производительности** — как работает система
- **Метрики доступности** — насколько доступна система для пользователей

## DORA-метрики

**DORA (DevOps Research and Assessment)** — индустриальный стандарт для оценки эффективности DevOps-процессов. На основе многолетних исследований были выделены 4 ключевые метрики, наиболее точно отражающие зрелость DevOps-практик.

### Deployment Frequency

**Определение:** Частота деплоев в production.

**Как считать:** Количество успешных деплоев в production за период времени (день/неделя/месяц).

**Целевые значения:**
- Elite: несколько раз в день
- High: между раз в день и раз в неделю
- Medium: между раз в неделю и раз в месяц
- Low: реже раза в месяц

**Как реализовать в TeamCity:**
- Создать отдельную сборку для deployment
- Настроить триггеры и условия успешного деплоя
- Использовать REST API для сбора статистики о частоте деплоев

**Пример запроса для Prometheus:**
```
sum(increase(deploy_count{environment="production", status="success"}[1w]))
```

### Lead Time for Changes

**Определение:** Время от первого коммита до успешного деплоя в production.

**Как считать:** Среднее время между первым коммитом в ветке и успешным деплоем в production.

**Целевые значения:**
- Elite: менее 1 дня
- High: между 1 днем и 1 неделей
- Medium: между 1 неделей и 1 месяцем
- Low: более 1 месяца

**Как реализовать в TeamCity:**
- Сохранять метаданные о коммитах
- Отслеживать время между первым коммитом и деплоем
- Создать кастомный отчет для визуализации

**Пример реализации:** См. [/teamcity-examples/lead-time.md](/teamcity-examples/lead-time.md)

### Change Failure Rate

**Определение:** Процент деплоев, приводящих к сбоям в production.

**Как считать:** (Количество деплоев, вызвавших сбой / Общее количество деплоев) × 100%

**Целевые значения:**
- Elite: 0-15%
- High: 16-30%
- Medium: 31-45%
- Low: 46-60%

**Как реализовать в TeamCity:**
- Маркировать сборки как "вызвавшие инцидент"
- Настроить интеграцию с системой мониторинга
- Создать кастомный отчет для расчета метрики

**Пример запроса для Prometheus:**
```
sum(deploy_incidents_count) / sum(deploy_count) * 100
```

### Mean Time to Recovery

**Определение:** Среднее время восстановления после сбоя в production.

**Как считать:** Среднее время между возникновением сбоя и его устранением.

**Целевые значения:**
- Elite: менее 1 часа
- High: между 1 часом и 1 днем
- Medium: между 1 днем и 1 неделей
- Low: более 1 недели

**Как реализовать в TeamCity + Grafana:**
- Интегрировать с системой мониторинга и алертинга
- Фиксировать время начала и окончания инцидентов
- Визуализировать MTTR на дашборде Grafana

**Пример Grafana dashboard:** См. [/grafana-examples/mttr-dashboard.json](/grafana-examples/mttr-dashboard.json)

### Сбор DORA-метрик в TeamCity

TeamCity не предоставляет DORA-метрики "из коробки", но их можно реализовать с помощью:

1. **Build Features и Triggers**:
   - Настроить метки для production деплоев
   - Использовать build chains для отслеживания процесса

2. **REST API**:
   - Собирать данные о сборках и деплоях
   - Рассчитывать метрики на основе этих данных

3. **Статистика и отчеты**:
   - Настроить кастомные отчеты
   - Экспортировать данные в Prometheus/Grafana

**Подробная инструкция:** См. [/docs/teamcity-dora-setup.md](/docs/teamcity-dora-setup.md)

## Ключевые метрики DevOps/SRE

Помимо DORA-метрик, существует множество других важных метрик для DevOps и SRE команд:

### Доступность и SLA/SLO/SLI

**SLI (Service Level Indicator)**:
- Метрики, отражающие фактический уровень сервиса
- Примеры: доступность, латентность, точность, пропускная способность

**SLO (Service Level Objective)**:
- Целевое значение для SLI
- Пример: "Доступность API должна быть не менее 99.9% в месяц"

**SLA (Service Level Agreement)**:
- Формальное соглашение с пользователями о качестве сервиса
- Обычно включает обязательства и штрафы

**Расчет доступности**:
```
Availability = (Total Time - Downtime) / Total Time * 100%
```

**Error Budget**:
- 100% - SLO = Error Budget (доступный бюджет ошибок)
- Позволяет балансировать скорость разработки и стабильность

**Примеры PromQL запросов:**
```
# Доступность сервиса (success rate)
sum(rate(http_requests_total{status=~"2.."}[5m])) / sum(rate(http_requests_total[5m]))

# Латентность (95-й процентиль)
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))
```

### Метрики производительности

**Latency (Задержка)**:
- Время обработки запроса
- Типично измеряется в процентилях (p50, p95, p99)

**Throughput (Пропускная способность)**:
- Количество запросов в единицу времени
- RPS (Requests Per Second), QPS (Queries Per Second)

**Saturation (Насыщение)**:
- Степень загрузки ресурсов
- CPU, Memory, Disk I/O, Network

**Errors (Ошибки)**:
- Количество и процент ошибок
- Ошибки HTTP 5xx, исключения, таймауты

**USE Method**:
- Utilization (Использование) — % времени, когда ресурс занят
- Saturation (Насыщение) — степень перегрузки ресурса
- Errors (Ошибки) — количество ошибок ресурса

**RED Method**:
- Rate (Частота) — запросов в секунду
- Errors (Ошибки) — % неудачных запросов
- Duration (Длительность) — распределение времени ответа

### Метрики инцидентов

**MTTR (Mean Time to Recovery)**:
- Среднее время от начала инцидента до восстановления

**MTTD (Mean Time to Detect)**:
- Среднее время от начала инцидента до его обнаружения

**MTTA (Mean Time to Acknowledge)**:
- Среднее время от оповещения до реакции команды

**MTBF (Mean Time Between Failures)**:
- Среднее время между инцидентами

**Расчет MTTR**:
```
MTTR = Σ(Время восстановления) / Количество инцидентов
```

**Расчет MTTD**:
```
MTTD = Σ(Время обнаружения - Время начала) / Количество инцидентов
```

### Метрики CI/CD Pipeline

**Build Success Rate**:
- % успешных сборок
- (Успешные сборки / Все сборки) × 100%

**Build Duration**:
- Время выполнения сборки
- Важно отслеживать тренд изменения

**Test Coverage**:
- % кода, покрытого тестами
- Помогает оценить качество тестирования

**Deployment Success Rate**:
- % успешных деплоев
- (Успешные деплои / Все деплои) × 100%

**Code Review Time**:
- Время от создания PR до его принятия
- Помогает выявить узкие места в процессе разработки

**Pipeline Velocity**:
- Скорость прохождения кода через пайплайн
- От коммита до продакшена

## Инструменты для сбора и визуализации

### TeamCity: настройка и сбор метрик

TeamCity предоставляет несколько способов для сбора метрик:

1. **Встроенные метрики**:
   - Build duration
   - Test count/failures
   - Build success rate
   - Code coverage

2. **REST API**:
   - Доступ к данным о проектах, сборках, тестах
   - Возможность создания кастомных метрик

3. **TeamCity Notification**:
   - Интеграция с внешними системами
   - Триггеры на события сборки

4. **Экспорт в Prometheus**:
   - Настройка TeamCity REST API exporter
   - Мониторинг метрик в реальном времени

**Пример настройки экспорта метрик из TeamCity в Prometheus:**
См. [/docs/teamcity-prometheus-export.md](/docs/teamcity-prometheus-export.md)

### Grafana: дашборды и визуализация

Grafana — мощный инструмент для визуализации метрик:

1. **Подключение источников данных**:
   - Prometheus для метрик производительности
   - PostgreSQL/MySQL для метрик из баз данных
   - API источники для JIRA, TeamCity, etc.

2. **Создание дашбордов**:
   - Предустановленные шаблоны
   - Кастомные панели и графики

3. **Алертинг**:
   - Настройка уведомлений на основе метрик
   - Интеграция с системами оповещения

**Готовые дашборды:**
- [DORA Metrics Dashboard](/grafana-examples/dora-dashboard.json)
- [SLO Dashboard](/grafana-examples/slo-dashboard.json)
- [TeamCity Performance Dashboard](/grafana-examples/teamcity-dashboard.json)

### Prometheus: запросы и сбор данных

Prometheus — система мониторинга с мощным языком запросов PromQL:

1. **Сбор метрик**:
   - Pull-модель (Prometheus сам опрашивает endpoints)
   - Поддержка различных форматов (OpenMetrics, Prometheus)

2. **Хранение данных**:
   - Time-series база данных
   - Эффективное хранение метрик

3. **PromQL**:
   - Мощный язык запросов
   - Агрегация, фильтрация, математические операции

**Примеры PromQL запросов:**
```
# Среднее время сборки в TeamCity
avg(teamcity_build_duration_seconds{project="MyProject"})

# 95-й процентиль времени ответа API
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, endpoint))

# Доступность сервиса за последнюю неделю
sum_over_time(up[7d]) / count_over_time(up[7d]) * 100
```

## Практические примеры

### TeamCity: отслеживание сборок

#### Настройка Build Feature для отслеживания метрик

```kotlin
// TeamCity DSL пример
object BuildFeatures {
    feature {
        type = "perfmon"
        param("monitoring.preset", "custom")
        param("monitoring.processes", "dotnet,java")
        param("monitoring.period", "1")
    }
}
```

#### Настройка статистических метрик

```
##teamcity[buildStatisticValue key='deploymentTime' value='120']
##teamcity[buildStatisticValue key='testsPassed' value='45']
```

#### Пример REST API запроса для получения метрик

```bash
curl -X GET "http://teamcity-server/app/rest/builds?locator=buildType:(id:MyProject_Build)&fields=build(id,status,statistics(property))"
```

### Grafana: готовые дашборды

#### Dashboard для DORA метрик

![DORA Metrics Dashboard](/docs/images/dora-dashboard.png)

Включает:
- Deployment Frequency
- Lead Time for Changes
- Change Failure Rate
- Mean Time to Recovery

#### Dashboard для SLO/SLI

![SLO Dashboard](/docs/images/slo-dashboard.png)

Включает:
- Доступность сервиса
- Латентность
- Error budget
- SLO status

### Prometheus: PromQL запросы

#### Запросы для метрик производительности

```
# CPU Usage
100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory Usage
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# Disk Usage
(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100
```

#### Запросы для метрик CI/CD

```
# Build Success Rate
sum(increase(teamcity_build_count{status="SUCCESS"}[1d])) / sum(increase(teamcity_build_count[1d])) * 100

# Average Build Duration
avg(teamcity_build_duration_seconds{buildType="MyProject_Build"})

# Deployment Frequency
count_over_time(teamcity_deployment_total{environment="production", status="SUCCESS"}[7d])
```

## Best practices

### Эффективное внедрение метрик

1. **Начинайте с малого**:
   - Выберите 3-5 ключевых метрик
   - Постепенно расширяйте набор

2. **Автоматизируйте сбор**:
   - Встраивайте сбор метрик в CI/CD
   - Минимизируйте ручной ввод данных

3. **Визуализируйте**:
   - Создавайте понятные дашборды
   - Используйте цветовую кодировку для быстрого восприятия

4. **Контекст важнее чисел**:
   - Сравнивайте с историческими данными
   - Устанавливайте реалистичные цели

5. **Используйте для улучшений**:
   - Анализируйте тренды и аномалии
   - Проводите регулярные ретроспективы на основе метрик

### Типичные ошибки

1. **Слишком много метрик**:
   - Информационная перегрузка
   - Сложность анализа

2. **Метрики ради метрик**:
   - Сбор данных без действий
   - Отсутствие связи с бизнес-целями

3. **Игнорирование контекста**:
   - Фокус только на числах
   - Непонимание причин изменений

4. **Неправильные стимулы**:
   - Использование метрик для наказания
   - Манипуляция метриками для достижения целей

5. **Отсутствие обратной связи**:
   - Метрики не влияют на процессы
   - Отсутствие цикла улучшений

## Полезные ссылки

### Документация и руководства

- [TeamCity Documentation](https://www.jetbrains.com/help/teamcity/teamcity-documentation.html)
- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)
- [Google DORA DevOps Research](https://cloud.google.com/devops)

### Книги и статьи

- [Site Reliability Engineering (Google)](https://sre.google/sre-book/table-of-contents/)
- [The DevOps Handbook](https://itrevolution.com/book/the-devops-handbook/)
- [Accelerate: The Science of Lean Software and DevOps](https://itrevolution.com/book/accelerate/)
- [State of DevOps Report](https://services.google.com/fh/files/misc/state-of-devops-2023.pdf)

### Инструменты и ресурсы

- [Sleuth DORA Metrics](https://sleuth.io/dora-metrics/)
- [Prometheus Exporter для TeamCity](https://github.com/JetBrains/teamcity-prometheus-exporter)
- [Grafana Labs: Building Better Dashboards](https://grafana.com/blog/2022/01/18/7-tips-for-designing-effective-grafana-dashboards/)

---

## Вклад в проект

Если у вас есть предложения по улучшению этого руководства, пожалуйста, создайте issue или pull request.

## Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.