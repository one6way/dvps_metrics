# DevOps & SRE Metrics Guide

## Оглавление
1. [Введение: зачем нужны метрики](#введение)
2. [DORA-метрики](#dora-метрики)
3. [Ключевые метрики DevOps/SRE](#ключевые-метрики-devopssre)
4. [Инструменты для сбора и визуализации](#инструменты-для-сбора-и-визуализации)
5. [Best practices внедрения метрик](#best-practices)
6. [Примеры расчёта и формулы](#примеры-расчёта)
7. [Полезные ссылки и литература](#полезные-ссылки)

---

## Введение
Метрики — ключ к объективной оценке эффективности процессов разработки, эксплуатации и улучшения качества сервиса. Они позволяют:
- Измерять скорость и стабильность изменений
- Находить узкие места
- Улучшать процессы на основе данных, а не ощущений
- Повышать доверие бизнеса к IT

## DORA-метрики
**DORA (DevOps Research and Assessment)** — индустриальный стандарт оценки эффективности DevOps-процессов:
- **Deployment Frequency** — частота деплоев (чем чаще, тем лучше)
- **Lead Time for Changes** — время от коммита до продакшн (чем меньше, тем лучше)
- **Change Failure Rate** — доля неудачных изменений (чем меньше, тем лучше)
- **Mean Time to Recovery (MTTR)** — среднее время восстановления после инцидента (чем меньше, тем лучше)

### Как измерять DORA-метрики
- Интеграция с CI/CD (GitHub Actions, GitLab, Jenkins)
- Использование Jira, Sleuth, LinearB, GitHub Insights, GitLab Analytics
- Примеры формул — см. ниже

## Ключевые метрики DevOps/SRE
- **MTTR/MTBF/MTTD/MTTA** — среднее время восстановления/между сбоями/обнаружения/реакции
- **Change Volume** — объём изменений
- **Release Frequency** — частота релизов
- **Change Lead Time** — время от идеи до релиза
- **Change Failure Rate** — % неудачных релизов
- **Incident Count** — количество инцидентов
- **Availability/Uptime (SLA/SLO/SLI)** — доступность сервиса
- **Error Rate** — частота ошибок
- **Deployment Success Rate** — % успешных деплоев
- **Code Review Time** — среднее время ревью
- **Cycle Time** — полный цикл задачи
- **Customer Ticket Volume** — обращения пользователей

## Инструменты для сбора и визуализации
- **CI/CD**: GitHub Actions, GitLab CI, Jenkins, ArgoCD
- **Issue Trackers**: Jira, YouTrack, LinearB
- **Мониторинг**: Prometheus, Grafana, Datadog, New Relic, Sentry, PagerDuty
- **DORA Tools**: Sleuth, LinearB, GitHub Insights, GitLab Analytics
- **Логи**: ELK/EFK, Loki

## Best practices
- Метрики — не для наказания, а для улучшения процессов
- Внедряйте метрики постепенно, не перегружайте команду
- Визуализируйте метрики (дашборды)
- Используйте метрики для ретроспектив и планирования
- Не забывайте про качество данных

## Примеры расчёта
- **Deployment Frequency**: количество деплоев в продакшн за неделю/месяц
- **Lead Time**: среднее время между первым коммитом и деплоем
- **Change Failure Rate**: (кол-во неудачных релизов / общее кол-во релизов) * 100%
- **MTTR**: среднее время между инцидентом и восстановлением
- **SLO/SLI**: SLI = (успешные запросы / все запросы) * 100%, SLO = целевое значение SLI
- **PromQL**: rate(http_requests_total{status!="200"}[5m])

## Полезные ссылки
- [Google DORA DevOps Research](https://cloud.google.com/devops)
- [Sleuth DORA Metrics](https://sleuth.io/dora-metrics/)
- [LinearB Engineering Metrics](https://linearb.io/engineering-metrics)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [State of DevOps Report](https://services.google.com/fh/files/misc/state-of-devops-2023.pdf) 