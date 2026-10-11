param([int]$Port = 8080, [string]$PublicBaseUrl = '')
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $repo 'qr-backend'
$envPath = Join-Path $backend '.env'
if (Test-Path -LiteralPath $envPath) {
    Write-Host 'Existing private .env preserved. Edit it locally if settings need to change.'
    exit 0
}
function New-Secret([int]$length = 32) {
    $bytes = New-Object byte[] $length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$private = @{}
$localPath = Join-Path $backend 'dev-local.properties'
if (Test-Path -LiteralPath $localPath) {
    foreach ($line in [IO.File]::ReadAllLines($localPath)) {
        $parts = $line.Split('=',2)
        if ($parts.Length -eq 2) { $private[$parts[0]] = $parts[1] }
    }
}
$jwt = $private['security.jwt.secret']
if (-not $jwt) { $jwt = New-Secret }
$voice = $private['voicerss.api-key']
if (-not $voice) { $voice = $env:VOICERSS_API_KEY }
if (-not $voice) {
    $secureKey = Read-Host 'VoiceRSS API key (hidden; Enter to configure later)' -AsSecureString
    $voice = [System.Net.NetworkCredential]::new('', $secureKey).Password
}
if (-not $PublicBaseUrl) {
    $address = Get-NetIPConfiguration -ErrorAction SilentlyContinue |
        Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address.IPAddress -notlike '169.254.*' } |
        Select-Object -First 1 -ExpandProperty IPv4Address
    $hostAddress = if ($address) { $address.IPAddress } else { 'localhost' }
    $PublicBaseUrl = 'http://' + $hostAddress + ':' + $Port
}
$lines = @(
    ('APP_PORT=' + $Port),
    'MYSQL_PORT=3308',
    ('MYSQL_PASSWORD=' + (New-Secret)),
    ('MYSQL_ROOT_PASSWORD=' + (New-Secret)),
    ('JWT_SECRET=' + $jwt),
    ('VOICERSS_API_KEY=' + $voice),
    'BOOTSTRAP_ADMIN_USERNAME=admin',
    ('BOOTSTRAP_ADMIN_PASSWORD=' + (New-Secret 18)),
    ('PUBLIC_BASE_URL=' + $PublicBaseUrl),
    'SEED_DEMO_DATA=true'
)
[IO.File]::WriteAllLines($envPath, $lines, [Text.UTF8Encoding]::new($false))
Write-Host 'Private qr-backend/.env created. No credential values are printed.'
Write-Host ('Backend address: ' + $PublicBaseUrl)
Write-Host 'Open .env locally to view the initial Admin password. Never commit this file.'
