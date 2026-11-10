$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$port = 8765
$url = "http://127.0.0.1:$port"
$healthUrl = "$url/api/health"
$logs = Join-Path $root 'logs'
$bin = Join-Path $root 'backend\bin'
$source = Join-Path $root 'backend\src\BeeSafeServer.java'
$compiled = Join-Path $bin 'BeeSafeServer.class'
New-Item -Path $logs -ItemType Directory -Force | Out-Null
New-Item -Path (Join-Path $root 'datos') -ItemType Directory -Force | Out-Null
function BeeSafeResponds {
  try {
    $h = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 2
    return ($h.app -eq 'BeeSafe beta' -and $h.ok -eq $true)
  } catch { return $false }
}
try {
  if (BeeSafeResponds) {
    Write-Host 'BeeSafe ya se encuentra ejecutandose.' -ForegroundColor Yellow
    Start-Process $url
    exit 0
  }
  $javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
  if (-not $javaCmd) { throw 'Java no aparece en el PATH. Instala un JDK 17 o superior (Temurin) y reinicia Windows.' }
  $java = $javaCmd.Source
  if (-not (Test-Path $compiled)) {
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if (-not $javac) { throw 'Falta BeeSafeServer.class y no se encontro javac. Instala un JDK 17 o superior.' }
    Write-Host 'Compilando servidor Java por primera vez...'
    & $javac.Source '--release' '17' '-encoding' 'UTF-8' '-d' $bin $source
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion del backend fallo.' }
  }
  $javaArgs = '-Dfile.encoding=UTF-8 -cp "' + $bin + '" BeeSafeServer'
  $out = Join-Path $logs 'beesafe-out.log'
  $err = Join-Path $logs 'beesafe-error.log'
  Write-Host 'Iniciando servidor BeeSafe...'
  $proc = Start-Process -FilePath $java -ArgumentList $javaArgs -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err -PassThru
  $pidFile = Join-Path $root 'datos\beesafe.pid'
  [IO.File]::WriteAllText($pidFile, [string]$proc.Id)
  $ok = $false
  for ($i = 0; $i -lt 48; $i++) {
    Start-Sleep -Milliseconds 300
    if (BeeSafeResponds) { $ok = $true; break }
    $proc.Refresh()
    if ($proc.HasExited) { break }
  }
  if (-not $ok) {
    $lastError = if (Test-Path $err) { (Get-Content $err -Tail 7) -join "`n" } else { 'Sin detalles' }
    throw "El servidor no respondio. Comprueba que el puerto $port este libre. Detalle: $lastError"
  }
  Write-Host 'BeeSafe disponible en:' $url -ForegroundColor Green
  Write-Host 'El navegador se abrira automaticamente.'
  Start-Process $url
  exit 0
} catch {
  Write-Host ''
  Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
  Write-Host 'Revisa los archivos de la carpeta logs si el error persiste.'
  exit 1
}
