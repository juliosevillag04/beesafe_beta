$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pidFile = Join-Path $root 'datos\beesafe.pid'
try {
  if (-not (Test-Path $pidFile)) {
    Write-Host 'No se encontro un proceso BeeSafe registrado.'
    exit 0
  }
  $savedPid = [int](Get-Content -Path $pidFile -Raw)
  $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $savedPid" -ErrorAction SilentlyContinue
  if ($processInfo -and $processInfo.CommandLine -match 'BeeSafeServer') {
    try {
      Invoke-RestMethod -Uri 'http://127.0.0.1:8765/api/shutdown' -Method Post -Headers @{ 'X-Demo-Role'='tecnico' } -ContentType 'application/json' -Body '{}' -TimeoutSec 3 | Out-Null
      Start-Sleep -Milliseconds 900
    } catch { }
    $stillAlive = Get-Process -Id $savedPid -ErrorAction SilentlyContinue
    if ($stillAlive) { Stop-Process -Id $savedPid -Force -ErrorAction Stop }
    Write-Host 'Servidor BeeSafe detenido. Los registros guardados permanecen en datos\estado.json.' -ForegroundColor Green
  } else {
    Write-Host 'El PID guardado ya no corresponde a BeeSafe; no se detuvo otro proceso.'
  }
  Remove-Item $pidFile -ErrorAction SilentlyContinue
  exit 0
} catch {
  Write-Host ('No se pudo detener BeeSafe: ' + $_.Exception.Message) -ForegroundColor Red
  exit 1
}
