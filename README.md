## Wallet Service

Микросервис для управления операциями с кошельками (пополнение и снятие средств) с использованием REST API.

### Технологии

- Java 17
- Spring Boot 3
- PostgreSQL
- Liquibase (для миграций базы данных)
- Docker и Docker Compose (для контейнеризации)

### Основные возможности

- Создание кошельков с уникальными UUID идентификаторами

- Пополнение и списание средств с кошельков

- Проверка баланса в реальном времени

- Поддержка высоких нагрузок (до 1000 RPS на один кошелек)

- Оптимистическая блокировка для обработки конкурентных операций

- Автоматические повторные попытки при конфликтах

### Запуск приложения

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/kunikhin/ItroomTestTask.git
   ```
2.  Создайте jar в папке target
   ```bash
   mvn clean package
   ```
3. Запустите приложение с помощью Docker Compose:
   ```bash
   docker-compose -f docker/docker-compose.yml up --build
   ```
Приложение будет доступно по адресу: `http://localhost:8080`

База данных будет доступна на порту: `5432`

### Настройка
Параметры приложения и базы данных можно настроить без пересборки контейнеров через переменные окружения. Для этого создайте файл .env в корневой директории (примеры переменных смотрите в docker-compose.yml).

Пример переменных для приложения (application.properties):

`SPRING_DATASOURCE_URL` - URL базы данных

`SPRING_DATASOURCE_USERNAME` - имя пользователя

`SPRING_DATASOURCE_PASSWORD` - пароль

`SERVER_PORT` - порт приложения (по умолчанию 8080)

Пример переменных для базы данных:

`POSTGRES_DB` - имя базы данных

`POSTGRES_USER` - пользователь

`POSTGRES_PASSWORD` - пароль

## API Endpoints
### Создание кошелька
**Method**: `POST`

**URL**: `/wallet/new`

### Пополнение или снятие средств
**Method**: `PATCH`

**URL**: `/api/v1/wallet`



**Request Body:**
```json
{
  "walletId": "UUID",
  "operationType": "DEPOSIT or WITHDRAW",
  "amount": 1000
}
```

### Запрос текущего баланса
**Method**: `GET`

**URL**: `/api/v1/wallets/{walletId}`




