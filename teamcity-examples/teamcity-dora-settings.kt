/**
 * TeamCity DSL для настройки DORA метрик
 * 
 * Этот файл содержит примеры настройки TeamCity для сбора DORA метрик:
 * - Deployment Frequency
 * - Lead Time for Changes
 * - Change Failure Rate
 * - Mean Time to Recovery
 * 
 * Используйте этот файл как шаблон для своих проектов.
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

// Определение проекта
version = "2021.2"

project {
    description = "Проект с настроенными DORA метриками"
    
    // Параметры для всего проекта
    params {
        param("env.PROMETHEUS_ENDPOINT", "http://prometheus:9090")
        param("env.GRAFANA_API_KEY", "credentialsJSON:grafana_api_key")
        param("env.TRACK_DORA_METRICS", "true")
    }
    
    // Конфигурация сборки и тестирования
    buildType(BuildAndTest)
    
    // Конфигурация деплоя в продакшн
    buildType(DeployToProduction)
    
    // Конфигурация для отслеживания инцидентов
    buildType(IncidentTracker)
    
    // Шаблон для DORA метрик
    template(DoraMetricsTemplate)
}

// Шаблон для DORA метрик
object DoraMetricsTemplate : Template({
    name = "DORA Metrics Template"
    description = "Шаблон с настройкой для сбора DORA метрик"
    
    params {
        // Параметры для DORA метрик
        param("env.DEPLOYMENT_ID", "")
        param("env.FIRST_COMMIT_TIMESTAMP", "")
        param("env.DEPLOYMENT_START_TIME", "")
        param("env.INCIDENT_ID", "")
    }
    
    // Обязательные шаги для сбора метрик
    steps {
        script {
            name = "Collect DORA Metrics"
            scriptContent = """
                #!/bin/bash
                
                # Текущее время
                NOW=$(date +%s)
                echo "##teamcity[setParameter name='env.CURRENT_TIME' value='$NOW']"
                
                # Если это деплой, сохраняем метрику deploymentCount
                if [[ "%env.IS_DEPLOYMENT%" == "true" ]]; then
                    echo "##teamcity[buildStatisticValue key='deploymentCount' value='1']"
                    
                    # Deployment Frequency
                    if [[ "%env.ENVIRONMENT%" == "production" ]]; then
                        echo "##teamcity[addBuildTag 'deployment']"
                        echo "##teamcity[addBuildTag 'environment:production']"
                    fi
                    
                    # Lead Time for Changes
                    if [[ -n "%env.FIRST_COMMIT_TIMESTAMP%" ]]; then
                        LEAD_TIME_SECONDS=$(( $NOW - %env.FIRST_COMMIT_TIMESTAMP% ))
                        echo "##teamcity[buildStatisticValue key='leadTimeSeconds' value='$LEAD_TIME_SECONDS']"
                        echo "Lead Time: $(( $LEAD_TIME_SECONDS / 3600 )) hours"
                    fi
                    
                    # Отправляем метрики в Prometheus
                    if [[ -n "%env.PROMETHEUS_ENDPOINT%" ]]; then
                        # Предполагается, что у вас есть скрипт для отправки метрик
                        ./send_to_prometheus.sh "deployment_count" "1" "environment=%env.ENVIRONMENT%"
                    fi
                fi
                
                # Если это восстановление после инцидента, сохраняем MTTR
                if [[ "%env.IS_RECOVERY%" == "true" && -n "%env.INCIDENT_START_TIME%" ]]; then
                    MTTR_SECONDS=$(( $NOW - %env.INCIDENT_START_TIME% ))
                    echo "##teamcity[buildStatisticValue key='mttrSeconds' value='$MTTR_SECONDS']"
                    echo "MTTR: $(( $MTTR_SECONDS / 60 )) minutes"
                    
                    # Отправляем метрики в Prometheus
                    if [[ -n "%env.PROMETHEUS_ENDPOINT%" ]]; then
                        ./send_to_prometheus.sh "mttr_seconds" "$MTTR_SECONDS" "incident_id=%env.INCIDENT_ID%"
                    fi
                fi
            """
        }
    }
    
    // Фичи для сбора метрик
    features {
        // Публикация статуса коммита для отслеживания изменений
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:github_token"
                }
            }
        }
    }
})

// Основная сборка и тестирование
object BuildAndTest : BuildType({
    name = "Build and Test"
    description = "Сборка и тестирование приложения"
    
    // Параметры для сборки
    params {
        param("env.IS_DEPLOYMENT", "false")
    }
    
    // Привязка к шаблону DORA
    templates(DoraMetricsTemplate)
    
    // VCS настройки
    vcs {
        root(DslContext.settingsRoot)
    }
    
    // Шаги сборки
    steps {
        // Сохраняем время первого коммита для расчета Lead Time
        script {
            name = "Get First Commit Timestamp"
            scriptContent = """
                #!/bin/bash
                
                # Получаем хеш первого коммита в ветке/PR
                FIRST_COMMIT=$(git log --format=format:%H --reverse %teamcity.build.branch% | head -1)
                
                # Получаем timestamp коммита
                COMMIT_TIMESTAMP=$(git show -s --format=%ct $FIRST_COMMIT)
                
                # Сохраняем как параметр сборки
                echo "##teamcity[setParameter name='env.FIRST_COMMIT_TIMESTAMP' value='$COMMIT_TIMESTAMP']"
                echo "##teamcity[setParameter name='env.FIRST_COMMIT_HASH' value='$FIRST_COMMIT']"
            """
        }
        
        // Ваши обычные шаги сборки и тестирования
        script {
            name = "Build Application"
            scriptContent = """
                echo "Building application..."
                # Ваш скрипт сборки
            """
        }
        
        script {
            name = "Run Tests"
            scriptContent = """
                echo "Running tests..."
                # Ваш скрипт тестирования
            """
        }
    }
    
    // Триггеры
    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }
    
    // Фичи
    features {
        pullRequests {
            provider = github {
                authType = token {
                    token = "credentialsJSON:github_token"
                }
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
            }
        }
    }
})

// Деплой в продакшн
object DeployToProduction : BuildType({
    name = "Deploy to Production"
    description = "Деплой приложения в продакшн"
    
    // Параметры для деплоя
    params {
        param("env.IS_DEPLOYMENT", "true")
        param("env.ENVIRONMENT", "production")
    }
    
    // Привязка к шаблону DORA
    templates(DoraMetricsTemplate)
    
    // VCS настройки
    vcs {
        root(DslContext.settingsRoot)
    }
    
    // Шаги деплоя
    steps {
        script {
            name = "Deploy to Production"
            scriptContent = """
                #!/bin/bash
                
                echo "Deploying to production..."
                # Ваш скрипт деплоя
                
                # Маркируем как деплой
                echo "##teamcity[addBuildTag 'deployment']"
                echo "##teamcity[addBuildTag 'environment:production']"
            """
        }
    }
    
    // Зависимости
    dependencies {
        // Запускаем деплой только после успешной сборки и тестирования
        dependency(BuildAndTest) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            
            // Передаем параметры из предыдущей сборки
            artifacts {
                artifactRules = "*.zip => ."
            }
        }
    }
    
    // Артефакты
    artifactRules = """
        deployment-log.txt
    """
})

// Трекер инцидентов
object IncidentTracker : BuildType({
    name = "Incident Tracker"
    description = "Отслеживание и регистрация инцидентов для метрик DORA"
    
    // Параметры для инцидентов
    params {
        param("env.IS_RECOVERY", "false")
        param("env.INCIDENT_START_TIME", "")
        param("env.INCIDENT_ID", "")
    }
    
    // Привязка к шаблону DORA
    templates(DoraMetricsTemplate)
    
    // VCS настройки
    vcs {
        root(DslContext.settingsRoot)
    }
    
    // Шаги для обработки инцидентов
    steps {
        script {
            name = "Process Incident"
            scriptContent = """
                #!/bin/bash
                
                # Текущий режим (register или resolve)
                MODE="%env.INCIDENT_MODE%"
                
                if [[ "$MODE" == "register" ]]; then
                    # Регистрация нового инцидента
                    INCIDENT_ID="INC-$(date +%Y%m%d%H%M%S)"
                    echo "Registering new incident: $INCIDENT_ID"
                    
                    # Время начала инцидента
                    NOW=$(date +%s)
                    
                    # Сохраняем в файл для последующего использования
                    echo "$INCIDENT_ID:$NOW" >> incidents.txt
                    
                    # Находим последний деплой перед инцидентом
                    ./find_deployment_before_incident.sh "$NOW"
                    
                    # Увеличиваем счетчик инцидентов
                    echo "##teamcity[buildStatisticValue key='incidentCount' value='1']"
                    
                elif [[ "$MODE" == "resolve" ]]; then
                    # Разрешение существующего инцидента
                    INCIDENT_ID="%env.INCIDENT_ID%"
                    echo "Resolving incident: $INCIDENT_ID"
                    
                    # Помечаем как восстановление
                    echo "##teamcity[setParameter name='env.IS_RECOVERY' value='true']"
                    
                    # Получаем время начала инцидента из файла
                    INCIDENT_START_TIME=$(grep "$INCIDENT_ID" incidents.txt | cut -d':' -f2)
                    echo "##teamcity[setParameter name='env.INCIDENT_START_TIME' value='$INCIDENT_START_TIME']"
                    
                    # Расчет MTTR
                    NOW=$(date +%s)
                    MTTR_SECONDS=$(( $NOW - $INCIDENT_START_TIME ))
                    echo "##teamcity[buildStatisticValue key='mttrSeconds' value='$MTTR_SECONDS']"
                    echo "MTTR: $(( $MTTR_SECONDS / 60 )) minutes"
                    
                    # Отправляем метрики в Prometheus
                    if [[ -n "%env.PROMETHEUS_ENDPOINT%" ]]; then
                        ./send_to_prometheus.sh "mttr_seconds" "$MTTR_SECONDS" "incident_id=$INCIDENT_ID"
                    fi
                fi
            """
        }
    }
    
    // Артефакты
    artifactRules = """
        incidents.txt
    """
}) 