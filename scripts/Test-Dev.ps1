param([string]$BaseUrl = 'http://localhost:8080', [switch]$WithAudio)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
function Get-Json([string]$Path) {
    try { return Invoke-RestMethod -Uri ($BaseUrl + $Path) -TimeoutSec 60 }
    catch { throw ('Request failed: ' + $Path + '. Check Docker health and configuration.') }
}
$health = Get-Json '/api/health'
if ($health.status -ne 'UP') { throw 'Database is not ready' }
foreach ($code in @('DEMO-OIL-LAMP','DEMO-BRONZE-DRUM','DEMO-CERAMIC-VASE')) {
    $item = (Get-Json ('/api/qrcodes/' + $code)).data.exhibit
    if ($item.languages.code -notcontains 'vi-vn' -or $item.languages.code -notcontains 'en-us') {
        throw ('Missing sample translation: ' + $code)
    }
    $qr = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/api/qrcodes/' + $code + '/image') -TimeoutSec 30
    if ($qr.Headers['Content-Type'] -notlike 'image/png*') { throw 'QR image failed' }
    foreach ($lang in @('vi-vn','en-us')) {
        $languageQr = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/api/qrcodes/' + $code + '/image?lang=' + $lang) -TimeoutSec 30
        if ($languageQr.Headers['Content-Type'] -notlike 'image/png*') { throw ('Language QR image failed: ' + $code + ' / ' + $lang) }
    }
}
try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/api/admin/users') -TimeoutSec 30
    throw 'Admin API unexpectedly allowed an anonymous request'
} catch {
    if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) {
        throw 'Expected HTTP 401 for anonymous Admin API'
    }
}
Write-Host 'PASS: health, three QR metadata APIs, Vietnamese/English translations, metadata/language PNG images, Admin protection.'
if ($WithAudio) {
    foreach ($code in @('DEMO-OIL-LAMP','DEMO-BRONZE-DRUM','DEMO-CERAMIC-VASE')) {
    foreach ($lang in @('vi-vn','en-us')) {
        $path = '/api/qrcodes/resolve/' + $code + '?lang=' + $lang
        $first = (Get-Json $path).data
        if ($first.languageCode -ne $lang -or -not $first.content -or -not $first.audioUrl) { throw 'Narration response is incomplete' }
        $audio = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $first.audioUrl) -TimeoutSec 60
        if ($audio.Headers['Content-Type'] -notlike 'audio/mpeg*' -or $audio.RawContentStream.Length -lt 100) { throw 'MP3 playback failed' }
        $range = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $first.audioUrl) -Headers @{Range='bytes=0-31'} -TimeoutSec 30
        if ($range.StatusCode -ne 206) { throw 'Audio seeking failed' }
        $second = (Get-Json $path).data
        if ($second.audioUrl -ne $first.audioUrl) { throw 'Cache did not reuse the audio URL' }
        $directUrl = $BaseUrl + '/api/qrcodes/' + $code + '/audio?lang=' + $lang
        $direct = Invoke-WebRequest -UseBasicParsing -Uri $directUrl -TimeoutSec 60
        if ($direct.Headers['Content-Type'] -notlike 'audio/mpeg*') { throw 'Direct QR audio is not MP3' }
        $hasher = [System.Security.Cryptography.SHA256]::Create()
        try {
            $audio.RawContentStream.Position = 0
            $direct.RawContentStream.Position = 0
            $cachedHash = [Convert]::ToBase64String($hasher.ComputeHash($audio.RawContentStream))
            $directHash = [Convert]::ToBase64String($hasher.ComputeHash($direct.RawContentStream))
        } finally { $hasher.Dispose() }
        if ($cachedHash -ne $directHash) { throw 'Direct QR audio differs from the chosen translation cache' }
        $directRange = Invoke-WebRequest -UseBasicParsing -Uri $directUrl -Headers @{Range='bytes=0-31'} -TimeoutSec 30
        if ($directRange.StatusCode -ne 206) { throw 'Direct QR audio seeking failed' }
        $directHead = Invoke-WebRequest -UseBasicParsing -Uri $directUrl -Method Head -TimeoutSec 30
        if ($directHead.StatusCode -ne 200 -or $directHead.Headers['Content-Type'] -notlike 'audio/mpeg*') { throw 'Direct QR audio HEAD failed' }
        Write-Host ('PASS: ' + $code + ' / ' + $lang + ' narration, cached/direct MP3 match, byte ranges, HEAD and repeated audio URL.')
    }
    }
}
