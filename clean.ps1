# clean.ps1
Write-Host "========================================" -ForegroundColor Red
Write-Host "ВНИМАНИЕ! Это удалит все контейнеры, volumes и данные!" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
$confirmation = Read-Host "Вы уверены? (yes/no)"

if ($confirmation -eq "yes") {
    Write-Host "Остановка и удаление..." -ForegroundColor Yellow
    docker-compose down -v
    Write-Host "Удаление образов..." -ForegroundColor Yellow
    docker rmi userservice:latest 2>$null
    Write-Host "Полная очистка выполнена" -ForegroundColor Green
} else {
    Write-Host "Операция отменена" -ForegroundColor Yellow
}