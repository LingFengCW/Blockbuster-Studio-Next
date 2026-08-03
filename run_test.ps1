# bbs-next auto test script
# Launches game via bat, monitors logs, reports results

$gameDir = "D:\E\MC\.minecraft\versions\luzhi"
$logFile = "$gameDir\logs\latest.log"
$batFile = "C:\Users\hechengyu\Desktop\启动 luzhi.bat"

# Clear old log
if (Test-Path $logFile) { Remove-Item $logFile -Force }

Write-Host "=== bbs-next Auto Test ===" -ForegroundColor Magenta
Write-Host "Launching game..." -ForegroundColor Cyan

# Launch via bat (hidden, no wait)
& $batFile

# Monitor log
$timeout = 180
$elapsed = 0
$crashed = $false
$playerEntered = $false

while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    
    # Check if game process is still running
    $mcProc = Get-Process -Name "javaw" -ErrorAction SilentlyContinue
    if (-not $mcProc) {
        Write-Host "Game process exited" -ForegroundColor Yellow
        break
    }
    
    if (Test-Path $logFile) {
        $content = Get-Content $logFile -Tail 100 -ErrorAction SilentlyContinue
        
        if ($content -match "Minecraft has crashed") {
            $crashed = $true
            Write-Host "`n!!! CRASH DETECTED !!!" -ForegroundColor Red
            $crashLines = Get-Content $logFile | Select-String "Mixin apply|Caused by:|Reported exception|Minecraft has crashed" -Context 0,2
            $crashLines | ForEach-Object { Write-Host "  $($_.Line)" -ForegroundColor Red }
            break
        }
        
        if ($content -match "joined the game" -and -not $playerEntered) {
            $playerEntered = $true
            Write-Host "Player entered the world!" -ForegroundColor Green
            if ($elapsed -gt 45) { break }
        }
    }
}

# Kill game
Get-Process -Name "javaw" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# Report
Write-Host "`n========================================" -ForegroundColor Magenta
Write-Host "           TEST REPORT" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

if (Test-Path $logFile) {
    $log = Get-Content $logFile
    Write-Host "Log lines: $($log.Count)" -ForegroundColor Gray
    
    if ($crashed) {
        Write-Host "Result: CRASHED" -ForegroundColor Red
    } elseif ($playerEntered) {
        Write-Host "Result: PASS (player entered world)" -ForegroundColor Green
    } else {
        Write-Host "Result: TIMEOUT (game did not fully start)" -ForegroundColor Yellow
    }
    
    Write-Host "`n--- Feature Checks ---" -ForegroundColor Cyan
    $checks = @(
        @("Components not bound yet", "ItemStack init timing"),
        @("InvalidAccessorException", "Mixin Accessor error"),
        @("NullPointerException.*shader", "ShaderProgram NPE"),
        @("Couldn't parse item model", "Item model format"),
        @("FileNotFoundException.*categories", "categories.json missing"),
        @("Watchdog.*shutdown", "WatchDog thread leak"),
        @("Custom game icon not applied", "Icon file missing"),
        @("formCategories", "FormCategories setup"),
        @("model.*was loaded", "Model loading"),
        @("script plugin", "ScriptPlugin loader")
    )
    
    foreach ($check in $checks) {
        $found = $log | Select-String $check[0]
        if ($found) {
            Write-Host "  [WARN] $($check[1])" -ForegroundColor Yellow
        } else {
            Write-Host "  [OK]   $($check[1])" -ForegroundColor Green
        }
    }
    
    # Count ERRORs
    $errorCount = ($log | Select-String "ERROR" | Measure-Object).Count
    Write-Host "`nTotal ERROR lines: $errorCount" -ForegroundColor $(if($errorCount -gt 15){"Red"}else{"Green"})
    
    # Show latest errors
    if ($errorCount -gt 0) {
        Write-Host "`n--- Last 10 ERROR lines ---" -ForegroundColor Yellow
        $log | Select-String "ERROR" | Select-Object -Last 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    }
}
