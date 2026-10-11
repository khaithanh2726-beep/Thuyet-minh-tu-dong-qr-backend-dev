param([Parameter(Mandatory=$true)][string]$Image, [string]$EnvFile = '')
$ErrorActionPreference = 'Stop'
if (-not $EnvFile) { $EnvFile = Join-Path $env:USERPROFILE '.qr-audio-dev/.env' }
if (-not (Test-Path -LiteralPath $EnvFile)) { throw 'Private DEV environment file is missing. See README.' }
$compose = Join-Path (Split-Path $PSScriptRoot -Parent) 'qr-backend/docker-compose.yml'
$previousImage = $env:APP_IMAGE
try {
    $env:APP_IMAGE = $Image
    docker compose --env-file $EnvFile -f $compose pull app db
    if ($LASTEXITCODE -ne 0) { throw 'Image download failed' }
    docker compose --env-file $EnvFile -f $compose up -d --no-build --wait --wait-timeout 240
    if ($LASTEXITCODE -ne 0) { throw 'DEV deployment did not become healthy' }
    Write-Host 'DEV deployment is healthy. Database and audio volumes were preserved.'
} finally { $env:APP_IMAGE = $previousImage }
