# start.ps1
Write-Host "========================================" -ForegroundColor Blue
Write-Host "Запуск UserService приложения" -ForegroundColor Blue
Write-Host "========================================`n" -ForegroundColor Blue

# Проверка Docker
$dockerCheck = docker --version 2>$null
if (-not $dockerCheck) {
    Write-Host "Ошибка: Docker не запущен или не установлен!" -ForegroundColor Red
    exit 1
}

# Проверка .env
if (-not (Test-Path ".env")) {
    Write-Host "Ошибка: Файл .env не найден!" -ForegroundColor Red
    exit 1
}

# Проверка init-скриптов
if (-not (Test-Path "init-scripts\01-create-app-user.sh")) {
    Write-Host "Ошибка: init-scripts\01-create-app-user.sh не найден!" -ForegroundColor Red
    exit 1
}

# Очистка старых контейнеров
Write-Host "Очистка старых контейнеров..." -ForegroundColor Yellow
docker-compose down -v

# Запуск контейнеров
Write-Host "Сборка и запуск контейнеров..." -ForegroundColor Yellow
docker-compose up -d --build

# Ожидание инициализации
Write-Host "Ожидание инициализации PostgreSQL (30 сек)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Проверка статуса
Write-Host "`nСтатус контейнеров:" -ForegroundColor Blue
docker-compose ps

# Проверка создания пользователя
Write-Host "`nПроверка создания пользователя app_user:" -ForegroundColor Blue
$appUser = docker-compose exec -T postgres psql -U postgres -d userservice -t -c "SELECT usename FROM pg_user WHERE usename = 'app_user';" 2>$null
if ($appUser.Trim() -eq "app_user") {
    Write-Host "✓ Пользователь app_user успешно создан" -ForegroundColor Green
} else {
    Write-Host "✗ Пользователь app_user не создан. Логи PostgreSQL:" -ForegroundColor Red
    docker-compose logs postgres --tail 30
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Запуск завершен!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "Для просмотра логов: docker-compose logs -f" -ForegroundColor Yellow
Write-Host "Для проверки: ./check.ps1" -ForegroundColor Yellow
Write-Host "Для остановки: ./stop.ps1" -ForegroundColor Yellow