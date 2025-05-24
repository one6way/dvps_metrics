# Настройка DORA метрик в TeamCity

Эта инструкция поможет вам настроить сбор и визуализацию DORA метрик (Deployment Frequency, Lead Time for Changes, Change Failure Rate, Mean Time to Recovery) в TeamCity и экспортировать их в Prometheus/Grafana.

## Содержание

- [Предварительные требования](#предварительные-требования)
- [Настройка Deployment Frequency](#настройка-deployment-frequency)
- [Настройка Lead Time for Changes](#настройка-lead-time-for-changes)
- [Настройка Change Failure Rate](#настройка-change-failure-rate)
- [Настройка MTTR](#настройка-mttr)
- [Экспорт метрик в Prometheus](#экспорт-метрик-в-prometheus)
- [Визуализация в Grafana](#визуализация-в-grafana)

## Предварительные требования

- TeamCity Server (версия 2020.2 или выше)
- Доступ к REST API TeamCity
- Prometheus (для хранения метрик)
- Grafana (для визуализации)

## Настройка Deployment Frequency

### Шаг 1: Маркировка деплой-сборок

Чтобы считать частоту деплоев, нам нужно однозначно идентифицировать деплой-сборки. В TeamCity это можно сделать несколькими способами:

1. **Создать отдельную конфигурацию сборки для деплоев:**

```kotlin
// TeamCity DSL
object DeploymentBuild : BuildType({
    name = "Production Deployment"
    
    triggers {
        // Trigger only after successful build of the main project
        vcs {
            triggerRules = "+:refs/tags/release-*"
        }
    }
    
    // Add deployment steps here
    steps {
        // ...
    }
    
    // Custom parameter to mark this as a deployment build
    params {
        param("env.IS_DEPLOYMENT", "true")
        param("env.ENVIRONMENT", "production")
    }
})
```

2. **Использовать теги сборки для маркировки деплоев:**

```bash
# В скрипте деплоя
echo "##teamcity[addBuildTag 'deployment']"
echo "##teamcity[addBuildTag 'environment:production']"
```

### Шаг 2: Сбор статистики деплоев

Для подсчета частоты деплоев, можно использовать TeamCity REST API или плагин для экспорта метрик.

#### Вариант 1: Скрипт для подсчета через REST API

```python
#!/usr/bin/env python3
import requests
import datetime
from datetime import timedelta
import json

# Настройки TeamCity
TEAMCITY_URL = "https://teamcity.your-company.com"
TEAMCITY_USER = "username"
TEAMCITY_PASSWORD = "password"

# Получаем деплои за последний месяц
def get_deployments(days=30):
    end_date = datetime.datetime.now()
    start_date = end_date - timedelta(days=days)
    
    start_date_str = start_date.strftime("%Y%m%dT%H%M%S%z")
    end_date_str = end_date.strftime("%Y%m%dT%H%M%S%z")
    
    # Получаем сборки с тегом deployment в production за указанный период
    url = f"{TEAMCITY_URL}/app/rest/builds?locator=tag:deployment,tag:environment:production,startDate:{start_date_str},endDate:{end_date_str},status:SUCCESS&fields=count"
    
    response = requests.get(url, auth=(TEAMCITY_USER, TEAMCITY_PASSWORD))
    data = response.json()
    
    return data["count"]

# Выводим результаты
deployments_count = get_deployments(30)
print(f"Deployment Frequency (last 30 days): {deployments_count}")
print(f"Average Deployments per week: {deployments_count / 4.3:.2f}")
```

#### Вариант 2: TeamCity Build Feature для хранения статистики

```kotlin
// TeamCity DSL
features {
    feature {
        type = "perfmon"
    }
    
    // В шаге сборки добавляем статистику
    steps {
        script {
            scriptContent = """
                echo "##teamcity[buildStatisticValue key='deploymentCount' value='1']"
            """
        }
    }
}
```

## Настройка Lead Time for Changes

Для отслеживания Lead Time (времени от коммита до деплоя), нам нужно:
1. Фиксировать время первого коммита в ветке/PR
2. Фиксировать время успешного деплоя
3. Рассчитывать разницу

### Шаг 1: Отслеживание времени первого коммита

```kotlin
// TeamCity DSL - Build Feature для хранения метаданных о коммитах
features {
    feature {
        type = "commit-status-publisher"
    }
}

// В шаге сборки добавляем скрипт для сохранения времени первого коммита
steps {
    script {
        scriptContent = """
            # Получаем хеш первого коммита в ветке
            FIRST_COMMIT=$(git log --format=format:%H --reverse %teamcity.build.branch% | head -1)
            
            # Получаем timestamp коммита
            COMMIT_TIMESTAMP=$(git show -s --format=%ct $FIRST_COMMIT)
            
            # Сохраняем как параметр сборки
            echo "##teamcity[setParameter name='env.FIRST_COMMIT_TIMESTAMP' value='$COMMIT_TIMESTAMP']"
            
            # Также сохраняем хеш коммита
            echo "##teamcity[setParameter name='env.FIRST_COMMIT_HASH' value='$FIRST_COMMIT']"
        """
    }
}
```

### Шаг 2: Расчет Lead Time при деплое

```kotlin
// В конфигурации деплоя добавляем расчет Lead Time
steps {
    script {
        scriptContent = """
            # Получаем текущее время в Unix timestamp
            DEPLOY_TIMESTAMP=$(date +%s)
            
            # Получаем timestamp первого коммита из параметров сборки
            FIRST_COMMIT_TIMESTAMP=%env.FIRST_COMMIT_TIMESTAMP%
            
            # Рассчитываем Lead Time в секундах
            LEAD_TIME_SECONDS=$((DEPLOY_TIMESTAMP - FIRST_COMMIT_TIMESTAMP))
            
            # Сохраняем метрику
            echo "##teamcity[buildStatisticValue key='leadTimeSeconds' value='$LEAD_TIME_SECONDS']"
            
            # Выводим информацию для наглядности
            LEAD_TIME_HOURS=$((LEAD_TIME_SECONDS / 3600))
            echo "Lead Time: $LEAD_TIME_HOURS hours"
        """
    }
}
```

## Настройка Change Failure Rate

Change Failure Rate — это процент деплоев, которые привели к сбоям или инцидентам. Для его отслеживания нам нужно:

1. Отмечать деплои, которые вызвали инциденты
2. Вести учет всех деплоев
3. Рассчитывать соотношение

### Шаг 1: Интеграция с системой отслеживания инцидентов

```kotlin
// TeamCity DSL
features {
    // Предположим, у нас есть HTTP webhook для получения инцидентов
    feature {
        type = "webhook-listener"
        param("url", "/api/incidents")
        param("events", "incident_created")
    }
}
```

### Шаг 2: Отметка деплоев, вызвавших инциденты

```bash
#!/bin/bash
# Скрипт для связывания инцидента с деплоем

# Параметры
TEAMCITY_URL="https://teamcity.your-company.com"
TEAMCITY_USER="username"
TEAMCITY_PASSWORD="password"
INCIDENT_ID=$1
INCIDENT_START_TIME=$2  # Unix timestamp

# Находим последний деплой перед инцидентом
curl -s -u $TEAMCITY_USER:$TEAMCITY_PASSWORD \
     "$TEAMCITY_URL/app/rest/builds?locator=tag:deployment,tag:environment:production,status:SUCCESS,finishDate:(date:$INCIDENT_START_TIME,condition:before),count:1" \
     -H "Accept: application/json" > last_deploy.json

BUILD_ID=$(jq -r '.build[0].id' last_deploy.json)

# Если деплой найден, отмечаем его как вызвавший инцидент
if [ ! -z "$BUILD_ID" ] && [ "$BUILD_ID" != "null" ]; then
    # Добавляем тег к сборке
    curl -s -X POST -u $TEAMCITY_USER:$TEAMCITY_PASSWORD \
         "$TEAMCITY_URL/app/rest/builds/id:$BUILD_ID/tags" \
         -H "Content-Type: text/plain" \
         -d "caused-incident:$INCIDENT_ID"
    
    # Обновляем статистику сборки
    curl -s -X PUT -u $TEAMCITY_USER:$TEAMCITY_PASSWORD \
         "$TEAMCITY_URL/app/rest/builds/id:$BUILD_ID/statistics/causedIncident" \
         -H "Content-Type: text/plain" \
         -d "1"
         
    echo "Linked incident $INCIDENT_ID to build $BUILD_ID"
else
    echo "No deployment found before incident $INCIDENT_ID"
fi
```

### Шаг 3: Расчет Change Failure Rate

```python
#!/usr/bin/env python3
import requests
import datetime
from datetime import timedelta

# Настройки TeamCity
TEAMCITY_URL = "https://teamcity.your-company.com"
TEAMCITY_USER = "username"
TEAMCITY_PASSWORD = "password"

# Функция для получения Change Failure Rate за указанный период
def get_change_failure_rate(days=30):
    end_date = datetime.datetime.now()
    start_date = end_date - timedelta(days=days)
    
    start_date_str = start_date.strftime("%Y%m%dT%H%M%S%z")
    end_date_str = end_date.strftime("%Y%m%dT%H%M%S%z")
    
    # Общее количество деплоев
    url_all = f"{TEAMCITY_URL}/app/rest/builds?locator=tag:deployment,tag:environment:production,startDate:{start_date_str},endDate:{end_date_str},status:SUCCESS&fields=count"
    
    # Количество деплоев, вызвавших инциденты
    url_failed = f"{TEAMCITY_URL}/app/rest/builds?locator=tag:deployment,tag:environment:production,startDate:{start_date_str},endDate:{end_date_str},status:SUCCESS,tag:caused-incident&fields=count"
    
    response_all = requests.get(url_all, auth=(TEAMCITY_USER, TEAMCITY_PASSWORD))
    response_failed = requests.get(url_failed, auth=(TEAMCITY_USER, TEAMCITY_PASSWORD))
    
    total_deployments = response_all.json()["count"]
    failed_deployments = response_failed.json()["count"]
    
    if total_deployments == 0:
        return 0
    
    # Расчет процента
    failure_rate = (failed_deployments / total_deployments) * 100
    
    return failure_rate

# Выводим результаты
failure_rate = get_change_failure_rate(30)
print(f"Change Failure Rate (last 30 days): {failure_rate:.2f}%")
```

## Настройка MTTR

Mean Time to Recovery (MTTR) — это среднее время от начала инцидента до его разрешения. Для отслеживания MTTR в TeamCity:

### Шаг 1: Интеграция с системой мониторинга/инцидентов

```kotlin
// TeamCity DSL - настройка интеграции с системой инцидентов
features {
    feature {
        type = "jira-integration"
        param("server", "https://jira.your-company.com")
        param("username", "teamcity-integration")
        param("password", "password")
        param("project", "INCIDENTS")
    }
}
```

### Шаг 2: Скрипт для расчета MTTR

```python
#!/usr/bin/env python3
import requests
import datetime
import statistics

# Настройки Jira
JIRA_URL = "https://jira.your-company.com"
JIRA_USER = "username"
JIRA_TOKEN = "token"

# Функция для расчета MTTR за указанный период
def calculate_mttr(days=30):
    # Получаем текущую дату и дату N дней назад
    end_date = datetime.datetime.now()
    start_date = end_date - datetime.timedelta(days=days)
    
    # Формируем запрос к Jira для получения закрытых инцидентов
    jql = f'project = INCIDENTS AND resolution is not EMPTY AND resolutiondate >= "{start_date.strftime("%Y-%m-%d")}" ORDER BY created DESC'
    
    # Запрос к Jira API
    url = f"{JIRA_URL}/rest/api/2/search"
    params = {
        "jql": jql,
        "maxResults": 100,
        "fields": "created,resolutiondate,customfield_10000"  # customfield_10000 - поле с временем обнаружения
    }
    
    response = requests.get(url, auth=(JIRA_USER, JIRA_TOKEN), params=params)
    data = response.json()
    
    if "issues" not in data or len(data["issues"]) == 0:
        return 0
    
    # Рассчитываем время восстановления для каждого инцидента
    recovery_times = []
    
    for issue in data["issues"]:
        created_str = issue["fields"]["created"]
        resolved_str = issue["fields"]["resolutiondate"]
        
        # Преобразуем строки в datetime объекты
        created = datetime.datetime.strptime(created_str, "%Y-%m-%dT%H:%M:%S.%f%z")
        resolved = datetime.datetime.strptime(resolved_str, "%Y-%m-%dT%H:%M:%S.%f%z")
        
        # Рассчитываем время восстановления в минутах
        recovery_time_minutes = (resolved - created).total_seconds() / 60
        recovery_times.append(recovery_time_minutes)
    
    # Рассчитываем среднее время восстановления
    mttr = statistics.mean(recovery_times)
    
    return mttr

# Выводим результаты
mttr_minutes = calculate_mttr(30)
print(f"MTTR (last 30 days): {mttr_minutes:.2f} minutes")
print(f"MTTR in hours: {mttr_minutes / 60:.2f}")
```

### Шаг 3: Сохранение MTTR в TeamCity

```bash
#!/bin/bash
# Скрипт для сохранения MTTR в TeamCity

# Запускаем Python-скрипт для расчета MTTR
MTTR_MINUTES=$(python3 calculate_mttr.py)

# Сохраняем как статистику сборки
echo "##teamcity[buildStatisticValue key='mttrMinutes' value='$MTTR_MINUTES']"
```

## Экспорт метрик в Prometheus

Для экспорта метрик из TeamCity в Prometheus можно использовать [TeamCity Prometheus Exporter](https://github.com/JetBrains/teamcity-prometheus-exporter) или собственный скрипт.

### Шаг 1: Настройка TeamCity Prometheus Exporter

1. Скачайте плагин с GitHub: https://github.com/JetBrains/teamcity-prometheus-exporter/releases
2. Установите плагин в TeamCity:
   - Перейдите в Administration > Plugins
   - Нажмите "Upload plugin zip"
   - Выберите скачанный ZIP-файл
   - Перезапустите TeamCity

3. Настройте метрики для экспорта в файле `<TeamCity Data Directory>/config/prometheus-metrics.properties`:

```properties
# Базовые метрики TeamCity
teamcity.builds.queued=true
teamcity.builds.running=true
teamcity.builds.completed=true

# Кастомные метрики для DORA
teamcity.metric.deploymentCount=true
teamcity.metric.leadTimeSeconds=true
teamcity.metric.causedIncident=true
teamcity.metric.mttrMinutes=true
```

### Шаг 2: Настройка Prometheus для сбора метрик

Добавьте в `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'teamcity'
    scrape_interval: 15s
    metrics_path: '/app/metrics'
    static_configs:
      - targets: ['teamcity-server:8111']
```

### Шаг 3: Создание кастомного экспортера для DORA метрик

Если встроенных возможностей недостаточно, можно создать отдельный экспортер:

```python
#!/usr/bin/env python3
from prometheus_client import start_http_server, Gauge, Counter
import time
import requests
import datetime
from datetime import timedelta

# Настройки
TEAMCITY_URL = "https://teamcity.your-company.com"
TEAMCITY_USER = "username"
TEAMCITY_PASSWORD = "password"
PROMETHEUS_PORT = 9101
SCRAPE_INTERVAL = 300  # 5 минут

# Метрики Prometheus
deployment_frequency = Gauge('teamcity_deployment_frequency_30d', 'Number of deployments in the last 30 days')
lead_time = Gauge('teamcity_lead_time_seconds', 'Average lead time for changes in seconds')
change_failure_rate = Gauge('teamcity_change_failure_rate_percent', 'Percentage of deployments causing incidents')
mttr = Gauge('teamcity_mttr_minutes', 'Mean time to recovery in minutes')

# Функции для сбора метрик
def get_deployment_frequency(days=30):
    end_date = datetime.datetime.now()
    start_date = end_date - timedelta(days=days)
    
    start_date_str = start_date.strftime("%Y%m%dT%H%M%S%z")
    end_date_str = end_date.strftime("%Y%m%dT%H%M%S%z")
    
    url = f"{TEAMCITY_URL}/app/rest/builds?locator=tag:deployment,tag:environment:production,startDate:{start_date_str},endDate:{end_date_str},status:SUCCESS&fields=count"
    
    response = requests.get(url, auth=(TEAMCITY_USER, TEAMCITY_PASSWORD))
    data = response.json()
    
    return data["count"]

# Функции для других метрик...

# Основной цикл сбора метрик
def collect_metrics():
    while True:
        # Собираем и обновляем метрики
        deployment_frequency.set(get_deployment_frequency())
        # Обновляем другие метрики...
        
        # Ждем до следующего сбора
        time.sleep(SCRAPE_INTERVAL)

if __name__ == '__main__':
    # Запускаем HTTP-сервер
    start_http_server(PROMETHEUS_PORT)
    # Запускаем сбор метрик
    collect_metrics()
```

## Визуализация в Grafana

После настройки экспорта метрик в Prometheus, можно создать дашборд в Grafana:

1. Добавьте Prometheus как источник данных в Grafana
2. Создайте новый дашборд с панелями для каждой DORA метрики:

### Пример PromQL-запросов для Grafana:

```
# Deployment Frequency (weekly)
sum(increase(teamcity_deployment_count{environment="production", status="success"}[1w]))

# Lead Time for Changes (hours)
avg(teamcity_lead_time_seconds) / 3600

# Change Failure Rate
sum(teamcity_caused_incident_count) / sum(teamcity_deployment_count) * 100

# MTTR
avg(teamcity_mttr_minutes)
```

### Визуализация целевых уровней

Добавьте горизонтальные линии на графики для визуализации целевых значений согласно классификации DORA:

```
# Deployment Frequency targets
# Elite: multiple per day (>=1 per day = 7+ per week)
# High: between once per day and once per week (1-7 per week)
# Medium: between once per week and once per month (0.25-1 per week)
# Low: less than once per month (<0.25 per week)

# Lead Time targets (hours)
# Elite: <24 hours
# High: between 24 hours and 1 week (24-168 hours)
# Medium: between 1 week and 1 month (168-720 hours)
# Low: >1 month (720+ hours)

# Change Failure Rate targets
# Elite: 0-15%
# High: 16-30%
# Medium: 31-45%
# Low: 46-60%

# MTTR targets (minutes)
# Elite: <1 hour (60 min)
# High: <1 day (1440 min)
# Medium: <1 week (10080 min)
# Low: >1 week (10080+ min)
```

## Заключение

В этом руководстве мы рассмотрели, как настроить сбор и визуализацию DORA метрик в среде TeamCity. Используя эти метрики, вы сможете объективно оценивать эффективность вашего DevOps-процесса и постоянно улучшать его.

Помните, что главная цель метрик — не просто их сбор, а использование их для непрерывного улучшения процессов разработки и доставки ПО. 