# Гайд по Docker: лучшие практики и примеры

## Оглавление
1. [Введение в Docker](#введение-в-docker)
2. [Базовые понятия Docker](#базовые-понятия-docker)
3. [Лучшие практики при работе с Docker](#лучшие-практики-для-dockerfile)
4. [Примеры Dockerfile](#примеры-dockerfile)
    - [Python (Flask)](#python-flask)
    - [Node.js (Express)](#nodejs-express)
    - [Go (микросервис)](#go-микросервис)
    - [Nginx (web server)](#nginx)
    - [Multi-stage build](#multi-stage-build)
    - [Bash/sh на разных Linux](#bashsh-на-разных-linux)
5. [Сравнение образов на разных Linux-базах](#сравнение-образов-на-разных-linux-базах)
6. [Полезные ссылки](#полезные-ссылки)
7. [Docker и CI/CD](#docker-и-cicd)

---

## Введение в Docker
Docker — это платформа для контейнеризации приложений, позволяющая упаковать приложение и все его зависимости в единый переносимый контейнер.

## Базовые понятия Docker
- **Образ (Image)** — шаблон для создания контейнеров.
- **Контейнер (Container)** — изолированный процесс, запущенный на основе образа.
- **Dockerfile** — файл с инструкциями для сборки образа.
- **Docker Hub** — публичный реестр образов.

## Лучшие практики для Dockerfile
- Используйте минимальные базовые образы (alpine, slim).
- Минимизируйте количество слоёв.
- Не храните секреты в образах.
- Используйте `.dockerignore`.
- Явно указывайте версии зависимостей.
- Используйте multi-stage build для production.
- Не запускайте приложения от root.

## Примеры Dockerfile

### Python (Flask)
[Пример](examples/python-flask/Dockerfile)

### Node.js (Express)
[Пример](examples/node-express/Dockerfile)

### Go (микросервис)
[Пример](examples/go-microservice/Dockerfile)

### Nginx
[Пример](examples/nginx/Dockerfile)

### Multi-stage build
[Пример](examples/multi-stage/Dockerfile)

### Bash/sh на разных Linux
[Пример](examples/bash-sh/Dockerfile)

## Сравнение образов на разных Linux-базах
- **Debian/Ubuntu** — удобно для совместимости, больше размер.
- **Alpine** — минимальный размер, musl libc (иногда несовместимость).
- **BusyBox** — ультра-минималистичный, только для простых задач.
- **Distroless** — только runtime, нет shell, безопаснее.

## Полезные ссылки
- [Официальная документация Docker](https://docs.docker.com/)
- [Best practices for writing Dockerfiles](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Awesome Docker](https://github.com/veggiemonk/awesome-docker)

## Docker и CI/CD

Контейнеризация отлично сочетается с CI/CD. Вот основные best practices и примеры интеграции Docker в автоматические пайплайны:

### Best practices для CI/CD с Docker
- Используйте `.dockerignore` для ускорения сборки и уменьшения размера образа.
- Кэшируйте слои (buildx, layer caching).
- Не храните секреты в Dockerfile и образах.
- Используйте сканеры уязвимостей (Trivy, Snyk) на этапе CI.
- Автоматизируйте тегирование (по git commit/tag, semver).
- Используйте сервисные аккаунты и храните credentials только в секретах CI/CD.
- Не запускайте контейнеры от root.

### Пример пайплайна для GitHub Actions
```yaml
name: Build and Push Docker image
on:
  push:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      - name: Login to DockerHub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: youruser/yourimage:latest
```

### Пример пайплайна для GitLab CI
```yaml
stages:
  - build
  - push

build-image:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
```

### Пример Jenkinsfile
```groovy
pipeline {
  agent any
  environment {
    REGISTRY = "docker.io"
    IMAGE = "youruser/yourimage"
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }
    stage('Build') {
      steps {
        sh 'docker build -t $IMAGE:$BUILD_NUMBER .'
      }
    }
    stage('Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
          sh 'docker push $IMAGE:$BUILD_NUMBER'
        }
      }
    }
  }
}
```

### Безопасность в CI/CD
- Встраивайте SAST/DAST/SCA в пайплайн.
- Используйте secret scanning (Trivy, Gitleaks).
- Минимизируйте права CI/CD runner'ов.
- Не используйте root в контейнерах. 