# ============================================================
#  FalseGod - headless one-click launcher (PowerShell version)
#  Purpose: start backend + frontend with NO popup window, NO pause,
#           logs redirected to *-run.log, exit as soon as ports ready.
#  Differs from start-headless.bat: uses Start-Process so children are
#  truly detached (bat "start /b" children die with the caller session).
#  Usage:
#     powershell -ExecutionPolicy Bypass -File start-headless.ps1
#  Stop with: stop-all.bat  (or .\stop-all.ps1)
#  NOTE: pure ASCII on purpose, safe under any console code page.
# ============================================================

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root 'false-god-backend'
$frontendDir = Join-Path $root 'html'
$jar = Join-Path $backendDir 'target\false-god-backend-1.0.0.jar'

function Write-Step($msg) { Write-Host $msg }

# Critical: host/IDE may inject SERVER__PORT; Spring relaxed binding would
# override 8080. Clear all of them in this process AND child environment.
foreach ($v in @('SERVER__PORT','SERVER__HOST','SERVER_PORT','SERVER_ADDRESS')) {
    Remove-Item "Env:$v" -ErrorAction SilentlyContinue
}

function Test-Port($port) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return [bool]$c
}

function Stop-Port($port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        $procId = $c.OwningProcess
        try {
            $p = Get-Process -Id $procId -ErrorAction Stop
            Write-Step ("    [kill] port {0} -> PID {1} ({2})" -f $port, $procId, $p.ProcessName)
            Stop-Process -Id $procId -Force -ErrorAction Stop
        } catch { }
    }
    Start-Sleep -Milliseconds 600
}

Write-Step '[headless] FalseGod headless launcher'

# ---- 1. Java ----
$javaCandidates = @(
    (Join-Path $env:JAVA_HOME 'bin\java.exe'),
    'C:\Program Files\Zulu\zulu-21\bin\java.exe',
    'C:\Program Files\Java\jdk-17\bin\java.exe'
)
$javaExe = $null
foreach ($c in $javaCandidates) {
    if ($c -and (Test-Path $c)) { $javaExe = $c; break }
}
if (-not $javaExe) {
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) { $javaExe = $cmd.Source }
}
if (-not $javaExe) { Write-Step '[X] Java not found'; exit 1 }
Write-Step "    [OK] Java: $javaExe"

# ---- 2. Maven (only needed when jar is missing) ----
$mvnCmd = $null
$mvnRoots = Get-ChildItem (Join-Path $env:USERPROFILE '.m2\wrapper\dists') -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue
foreach ($r in $mvnRoots) {
    $found = Get-ChildItem $r.FullName -Recurse -Filter 'mvn.cmd' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $mvnCmd = $found.FullName; break }
}
if (-not $mvnCmd) {
    $cmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($cmd) { $mvnCmd = $cmd.Source }
}

# ---- 3. Node / npm ----
$npmExe = 'npm.cmd'
if (-not (Get-Command $npmExe -ErrorAction SilentlyContinue)) {
    Write-Step '[X] npm not found'; exit 1
}

# ---- 4. Free ports ----
Stop-Port 5173
Stop-Port 8080

# ---- 5. MySQL + database ----
$mysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path $mysqlExe)) {
    $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($cmd) { $mysqlExe = $cmd.Source } else { $mysqlExe = $null }
}
if (-not (Test-Port 3306)) {
    Write-Step '    [..] MySQL not running, trying to start service'
    Start-Service 'MySQL80' -ErrorAction SilentlyContinue
    for ($i = 0; $i -lt 10 -and -not (Test-Port 3306); $i++) { Start-Sleep -Milliseconds 800 }
}
if (-not (Test-Port 3306)) { Write-Step '[X] MySQL not listening on 3306'; exit 1 }
Write-Step '    [OK] MySQL :3306'

if ($mysqlExe) {
    $probe = & $mysqlExe -uroot -p123456 -N -e "use information_schema; select table_name from tables where table_schema='false_god' and table_name='t_message';" 2>$null
    if (-not ($probe -match 't_message')) {
        Write-Step '    [..] database incomplete, initializing'
        & $mysqlExe -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4;" 2>$null | Out-Null
        foreach ($f in @('false_god.sql','false_god_content.sql','false_god_social.sql','false_god_power.sql','false_god_task.sql')) {
            $sqlPath = Join-Path $backendDir "sql\$f"
            if (Test-Path $sqlPath) {
                cmd /c "`"$mysqlExe`" -uroot -p123456 --default-character-set=utf8mb4 false_god < `"$sqlPath`"" 2>$null | Out-Null
            }
        }
        Write-Step '    [OK] database initialized'
    }
}

# ---- 6. Build backend if jar missing ----
if (-not (Test-Path $jar)) {
    if (-not $mvnCmd) { Write-Step '[X] jar missing and no Maven'; exit 1 }
    Write-Step '    [..] jar missing, building with Maven'
    Push-Location $backendDir
    & $mvnCmd -q -B -DskipTests clean package
    $buildOk = (Test-Path $jar)
    Pop-Location
    if (-not $buildOk) { Write-Step '[X] build failed'; exit 1 }
}
Write-Step '    [OK] jar ready'

# ---- 7. Start backend (detached, log redirected) ----
$backendLog = Join-Path $backendDir 'backend-run.log'
Write-Step '    [..] starting backend :8080'
$backendArgs = @('-jar', $jar, '--server.port=8080', '--server.address=0.0.0.0')
Start-Process -FilePath $javaExe -ArgumentList $backendArgs -WorkingDirectory $backendDir `
    -RedirectStandardOutput $backendLog -RedirectStandardError (Join-Path $backendDir 'backend-err.log') `
    -WindowStyle Hidden | Out-Null

for ($i = 0; $i -lt 60; $i++) {
    if (Test-Port 8080) { break }
    Start-Sleep -Milliseconds 800
}
if (Test-Port 8080) { Write-Step '    [OK] backend :8080' }
else {
    Write-Step '[X] backend timeout, see backend-run.log'
    if (Test-Path $backendLog) { Get-Content $backendLog -Tail 25 }
    exit 1
}

# ---- 8. Start frontend (detached, log redirected) ----
if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Write-Step '    [..] node_modules missing, running npm install'
    Push-Location $frontendDir
    & $npmExe install
    if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
        Pop-Location; Write-Step '[X] npm install failed'; exit 1
    }
    Pop-Location
}

$frontendLog = Join-Path $frontendDir 'frontend-run.log'
Write-Step '    [..] starting frontend :5173'
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'npm run dev' -WorkingDirectory $frontendDir `
    -RedirectStandardOutput $frontendLog -RedirectStandardError (Join-Path $frontendDir 'frontend-err.log') `
    -WindowStyle Hidden | Out-Null

for ($i = 0; $i -lt 40; $i++) {
    if (Test-Port 5173) { break }
    Start-Sleep -Milliseconds 800
}
if (Test-Port 5173) { Write-Step '    [OK] frontend :5173' }
else {
    Write-Step '[X] frontend timeout, see frontend-run.log'
    if (Test-Path $frontendLog) { Get-Content $frontendLog -Tail 20 }
    exit 1
}

Write-Step ''
Write-Step '  ============================================================'
Write-Step '   [DONE] frontend http://192.168.68.63:5173'
Write-Step '          backend  http://192.168.68.63:8080'
Write-Step '   test account: chenhaoyu / 123456'
Write-Step '  ============================================================'
exit 0
