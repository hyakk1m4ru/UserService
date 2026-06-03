# stop.ps1
Write-Host "Остановка контейнеров..." -ForegroundColor Yellow
docker-compose down
Write-Host "Контейнеры остановлены" -ForegroundColor Green