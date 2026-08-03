# Test runner for bbs-next MC 26.2
# Launches the game and monitors for crashes, then reports results.

param(
    [switch]$Wait = $false
)

$gameDir = "D:\E\MC\.minecraft\versions\luzhi"
$logFile = "$gameDir\logs\latest.log"

# Clear old log
if (Test-Path $logFile) { Remove-Item $logFile -Force }

# Start the game
Write-Host "Starting Minecraft..." -ForegroundColor Cyan
$proc = Start-Process -FilePath "D:\Program Files\Java\jdk-26.0.1\bin\javaw.exe" -ArgumentList @(
    "-Dfile.encoding=COMPAT",
    "-Dstderr.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "-javaagent:`"D:\E\MC\PCL\PCL\lwjgl-unsafe-agent.jar`"",
    "-XX:+UseG1GC",
    "-XX:-UseAdaptiveSizePolicy",
    "-XX:-OmitStackTraceInFastThrow",
    "-Djdk.lang.Process.allowAmbiguousCommands=true",
    "-Dfml.ignoreInvalidMinecraftCertificates=True",
    "-Dfml.ignorePatchDiscrepancies=True",
    "-Dlog4j2.formatMsgNoLookups=true",
    "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump",
    "--sun-misc-unsafe-memory-access=allow",
    "--enable-native-access=ALL-UNNAMED",
    "-Djava.library.path=$gameDir\luzhi-natives\java",
    "-Djna.tmpdir=$gameDir\luzhi-natives\jna",
    "-Dorg.lwjgl.system.SharedLibraryExtractPath=$gameDir\luzhi-natives\lwjgl",
    "-Dio.netty.native.workdir=$gameDir\luzhi-natives\netty",
    "-Dminecraft.launcher.brand=PCLCE",
    "-Dminecraft.launcher.version=514",
    "-cp", "`"$gameDir\luzhi.jar`"",
    "-DFabricMcEmu=net.minecraft.client.main.Main",
    "-Xmn230m",
    "-Xmx1536m",
    "net.fabricmc.loader.impl.launch.knot.KnotClient",
    "--username", "thewindows11",
    "--version", "luzhi",
    "--gameDir", $gameDir,
    "--assetsDir", "D:\E\MC\.minecraft\assets",
    "--assetIndex", "32",
    "--uuid", "6f60266283164a6a89bde503ca7dc541",
    "--accessToken", "eyJraWQiOiJhY2NvdW50cy5hY2Nlc3N0b2tlbiIsInR5cCI6IkpXVCJ9.eyJ4dWlkIjoiMjUzNTQ2NTgxNTM1NjI2MCIsImFnZyI6IkFkdWx0Iiwic3ViIjoiNmY2MDI2NjItODMxNi00YTZhLTg5YmQtZTUwM2NhN2RjNTQxIiwianRpIjoiNTQyYzEwZDYtMTAwMS00MDc5LWJjN2QtNTQxMjc4OTc4Y2E3IiwibmJmIjoxNzUwMjY4MjgyLCJpc3MiOiJBY2NvdW50cyBTZXJ2aWNlIiwiZXhwIjoxNzUwMjcwMDgyLCJpYXQiOjE3NTAyNjgyODIsImZsbyI6MX0.0yNBv_fOYa-f9q7pCACZqLLjkqIGAJBWMkHi82lH7yWvw",
    "--clientId", '${clientid}',
    "--xuid", '${auth_xuid}',
    "--versionType", "PCLCE",
    "--width", "854",
    "--height", "480"
) -WindowStyle Hidden -PassThru

Write-Host "Game started (PID: $($proc.Id)), waiting for log output..." -ForegroundColor Cyan

# Wait and monitor log for crashes
$timeout = 120  # seconds
$elapsed = 0
$crashed = $false
$result = @{}

while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 2
    $elapsed += 2
    
    if ($proc.HasExited) {
        Write-Host "Game process exited with code $($proc.ExitCode)" -ForegroundColor Yellow
        break
    }
    
    if (Test-Path $logFile) {
        $content = Get-Content $logFile -Tail 50
        if ($content -match "Minecraft has crashed!" -or $content -match "Reported exception thrown" -or $content -match "FATAL") {
            $crashed = $true
            Write-Host "CRASH DETECTED!" -ForegroundColor Red
            break
        }
        if ($content -match "thewindows11加入了游戏" -or $content -match "logged in with entity id") {
            Write-Host "Player successfully entered the world!" -ForegroundColor Green
            # Keep monitoring for a bit to catch late crashes
            if ($elapsed -gt 30) { break }
        }
    }
}

# Stop the game if still running
if (-not $proc.HasExited) {
    Write-Host "Stopping game..." -ForegroundColor Yellow
    $proc.Kill()
}

# Report results
Write-Host "`n=== TEST RESULTS ===" -ForegroundColor Magenta
if (Test-Path $logFile) {
    $log = Get-Content $logFile
    $errors = $log | Select-String "ERROR" | Select-Object -First 20
    $crashes = $log | Select-String "crashed|Crash|FATAL" | Select-Object -First 10
    
    Write-Host "Total log lines: $($log.Count)" -ForegroundColor Gray
    Write-Host "Errors found: $($errors.Count)" -ForegroundColor $(if($errors.Count -gt 5){"Red"}else{"Green"})
    
    if ($crashes.Count -gt 0) {
        Write-Host "CRASHES:" -ForegroundColor Red
        $crashes | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    } else {
        Write-Host "No crashes detected!" -ForegroundColor Green
    }
    
    # Check for key features
    $features = @{
        "Components not bound" = "ItemStack init timing"
        "Custom game icon" = "GameIconPlugin"
        "script plugin" = "ScriptPlugin loader"
        "model.*was loaded" = "Model loading"
        "formCategories" = "FormCategories setup"
    }
    
    Write-Host "`nFeature checks:" -ForegroundColor Magenta
    foreach ($pattern in $features.Keys) {
        $found = $log | Select-String $pattern
        $status = if ($found) { "PASS" } else { "N/A" }
        $color = if ($status -eq "PASS") { "Green" } else { "Gray" }
        Write-Host "  [$status] $($features[$pattern])" -ForegroundColor $color
    }
}
