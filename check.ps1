# check.ps1
Write-Host "========================================" -ForegroundColor Blue
Write-Host "Проверка работоспособности" -ForegroundColor Blue
Write-Host "========================================`n" -ForegroundColor Blue

# 1. Статус контейнеров
Write-Host "1. Статус контейнеров:" -ForegroundColor Blue
docker-compose ps

# 2. Проверка пользователя БД
Write-Host "`n2. Проверка пользователя БД:" -ForegroundColor Blue
$appUser = docker-compose exec -T postgres psql -U postgres -d userservice -t -c "SELECT usename FROM pg_user WHERE usename = 'app_user';" 2>$null
if ($appUser.Trim() -eq "app_user") {
    Write-Host "✓ Пользователь app_user создан" -ForegroundColor Green
} else {
    Write-Host "✗ Пользователь app_user НЕ создан" -ForegroundColor Red
    Write-Host "Проверьте логи PostgreSQL:" -ForegroundColor Yellow
    docker-compose logs postgres --tail 20
}

# 3. Проверка прав доступа
Write-Host "`n3. Проверка прав доступа:" -ForegroundColor Blue
$createTest = docker-compose exec -T postgres psql -U app_user -d userservice -c "CREATE TABLE test123 (id int);" 2>&1
if ($createTest -like "*permission denied*") {
    Write-Host "✓ CREATE TABLE запрещен (хорошо)" -ForegroundColor Green
} else {
    Write-Host "✗ ВНИМАНИЕ: app_user может создавать таблицы!" -ForegroundColor Red
}

# 4. Проверка SELECT для app_user
Write-Host "`n4. Проверка SELECT (должен работать):" -ForegroundColor Blue
$selectTest = docker-compose exec -T postgres psql -U app_user -d userservice -c "SELECT NOW();" 2>&1
if ($selectTest -like "*ERROR*") {
    Write-Host "✗ SELECT не работает для app_user!" -ForegroundColor Red
} else {
    Write-Host "✓ SELECT работает" -ForegroundColor Green
}

# 5. Проверка подключения приложения
Write-Host "`n5. Проверка приложения:" -ForegroundColor Blue
$logs = docker-compose logs app --tail 30 2>$null
if ($logs -like "*Started*") {
    Write-Host "✓ Приложение запущено" -ForegroundColor Green
} elseif ($logs -like "*Connection refused*" -or $logs -like "*Access denied*") {
    Write-Host "✗ Проблемы с подключением к БД!" -ForegroundColor Red
    Write-Host $logs | Select-String -Pattern "error|exception|denied" -CaseSensitive:$false
} else {
    Write-Host "! Приложение возможно еще запускается" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "Проверка завершена!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Blue

