param([string]$CacheDirectory)
$ErrorActionPreference = 'Stop'
if (-not $CacheDirectory) { $CacheDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'target\temurin-21.0.12.1' }
$archive = Join-Path $CacheDirectory 'temurin-jre.zip'
$runtime = Join-Path $CacheDirectory 'jdk-21.0.12.1+1-jre'
$checksum = 'd35f31e712f0fcf6ac5a093edc90204fbff22f720ba3950bd09d331d5e621636'
$url = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip'
New-Item -ItemType Directory -Path $CacheDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $archive)) {
    Write-Host 'Downloading Eclipse Temurin 21 Windows x64 runtime...'
    Invoke-WebRequest -Uri $url -OutFile $archive
}
if ((Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant() -ne $checksum) {
    throw 'Temurin checksum mismatch. Remove the cached ZIP and retry.'
}
if (-not (Test-Path -LiteralPath (Join-Path $runtime 'bin\java.exe'))) {
    Expand-Archive -LiteralPath $archive -DestinationPath $CacheDirectory
}
if (-not (Test-Path -LiteralPath (Join-Path $runtime 'legal\java.base\LICENSE'))) { throw 'Runtime license files are missing.' }
Write-Output $runtime
