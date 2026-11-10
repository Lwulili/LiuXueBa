param(
    [ValidateSet('Start', 'Stop', 'Status')]
    [string]$Action = 'Status',
    [string]$PostgresBin = (Join-Path $env:LOCALAPPDATA 'LiuXueBa\postgresql-18.6\pgsql\bin'),
    [string]$DataDirectory = (Join-Path $env:LOCALAPPDATA 'LiuXueBa\postgres-data')
)

$ErrorActionPreference = 'Stop'
$pgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
if (-not (Test-Path -LiteralPath $pgCtl)) {
    throw 'PostgreSQL runtime was not found. Supply -PostgresBin or use your installed PostgreSQL service.'
}
if (-not (Test-Path -LiteralPath (Join-Path $DataDirectory 'PG_VERSION'))) {
    throw 'This local PostgreSQL data directory has not been initialized.'
}

if ($Action -eq 'Status') {
    & $pgCtl -D $DataDirectory status
    exit $LASTEXITCODE
}

if ($Action -eq 'Start') {
    & $pgCtl -D $DataDirectory status *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Output 'Local PostgreSQL is already running.'
        exit 0
    }
    $logFile = Join-Path (Split-Path -Parent $DataDirectory) 'postgres.log'
    $controlArguments = @('-D', ('"{0}"' -f $DataDirectory), '-l', ('"{0}"' -f $logFile), '-w', 'start')
} else {
    $controlArguments = @('-D', ('"{0}"' -f $DataDirectory), '-m', 'fast', '-w', 'stop')
}

$controlProcess = Start-Process -FilePath $pgCtl -ArgumentList $controlArguments -WindowStyle Hidden -PassThru
$controlProcess.WaitForExit()
if ($controlProcess.ExitCode -ne 0) {
    throw "PostgreSQL $Action failed. Check the local PostgreSQL log."
}
Write-Output "Local PostgreSQL $Action completed."
exit 0
