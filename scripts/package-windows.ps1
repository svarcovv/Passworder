param([string]$Destination = 'dist')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
        $jdkBin = Join-Path $env:JAVA_HOME 'bin'
    } else {
        $javaCommand = (Get-Command java -ErrorAction Stop).Source
        $probeInfo = New-Object System.Diagnostics.ProcessStartInfo
        $probeInfo.FileName = $javaCommand
        $probeInfo.Arguments = '-XshowSettings:properties -version'
        $probeInfo.UseShellExecute = $false
        $probeInfo.CreateNoWindow = $true
        $probeInfo.RedirectStandardError = $true
        $javaProbe = [System.Diagnostics.Process]::Start($probeInfo)
        $javaSettings = $javaProbe.StandardError.ReadToEnd()
        $javaProbe.WaitForExit()
        $javaProbe.Dispose()
        $jdkHomeMatch = [regex]::Match($javaSettings, 'java.home\s*=\s*([^\r\n]+)')
        if (-not $jdkHomeMatch.Success) { throw 'JDK not found. Set JAVA_HOME to JDK 21.' }
        $jdkBin = Join-Path $jdkHomeMatch.Groups[1].Value.Trim() 'bin'
    }
    $jpackage = Join-Path $jdkBin 'jpackage.exe'
    $jlink = Join-Path $jdkBin 'jlink.exe'
    if (-not (Test-Path -LiteralPath $jpackage)) { throw 'JDK 21 with jpackage is required.' }
    & .\mvnw.cmd -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
    $packageInput = Join-Path $projectRoot 'target\package-input'
    New-Item -ItemType Directory -Path $packageInput -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'target\Passworder.jar') -Destination $packageInput
    Copy-Item -LiteralPath (Join-Path $projectRoot 'THIRD_PARTY.md') -Destination $packageInput
    $runtimePath = Join-Path $projectRoot 'target\runtime'
    if (-not (Test-Path -LiteralPath $runtimePath)) {
        & $jlink --add-modules java.base,java.desktop,java.logging,java.prefs,jdk.unsupported,jdk.crypto.ec --strip-debug --no-header-files --no-man-pages --compress=2 --output $runtimePath
        if ($LASTEXITCODE -ne 0) { throw 'Failed to build the Java runtime.' }
    }
    $destPath = [IO.Path]::GetFullPath((Join-Path $projectRoot $Destination))
    if (Test-Path -LiteralPath (Join-Path $destPath 'Passworder')) { throw 'Destination already contains Passworder. Use -Destination with a new directory.' }
    & $jpackage --type app-image --name Passworder --app-version 0.1.0 --vendor svarcovv --icon src/main/resources/icons/passworder.ico --input $packageInput --main-jar Passworder.jar --main-class svarcovv.dev.passworder.Passworder --runtime-image $runtimePath --dest $destPath --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
    foreach ($document in @('README.md', 'SECURITY.md', 'THIRD_PARTY.md')) {
        Copy-Item -LiteralPath (Join-Path $projectRoot $document) -Destination (Join-Path $destPath 'Passworder')
    }
    Write-Host "Built: $(Join-Path $destPath 'Passworder\Passworder.exe')"
} finally { Pop-Location }
