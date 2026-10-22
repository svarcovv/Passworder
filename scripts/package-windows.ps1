param([string]$Destination = 'dist', [string]$RuntimeImage)
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
    if (-not (Test-Path -LiteralPath $jpackage)) { throw 'JDK 21 with jpackage is required.' }
    & .\mvnw.cmd -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
    $appVersion = ([xml](Get-Content -LiteralPath pom.xml -Raw)).project.version
    $packageInput = Join-Path $projectRoot "target\package-input-$appVersion"
    New-Item -ItemType Directory -Path $packageInput -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'target\Passworder.jar') -Destination $packageInput
    $runtimePath = if ($RuntimeImage) { (Resolve-Path -LiteralPath $RuntimeImage).Path } else { & (Join-Path $PSScriptRoot 'get-runtime-windows.ps1') }
    if (-not (Test-Path -LiteralPath (Join-Path $runtimePath 'bin\java.exe'))) { throw 'Invalid runtime image.' }
    $destPath = [IO.Path]::GetFullPath((Join-Path $projectRoot $Destination))
    if (Test-Path -LiteralPath (Join-Path $destPath 'Passworder')) { throw 'Destination already contains Passworder. Use -Destination with a new directory.' }
    & $jpackage --type app-image --name Passworder --app-version $appVersion --vendor svarcovv --icon src/main/resources/icons/passworder.ico --input $packageInput --main-jar Passworder.jar --main-class svarcovv.dev.passworder.Passworder --runtime-image $runtimePath --dest $destPath --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
    $legalDestination = Join-Path $destPath 'Passworder\licenses'
    New-Item -ItemType Directory -Path $legalDestination -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'THIRD_PARTY.md') -Destination (Join-Path $legalDestination 'ThirdParty.txt')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'RUNTIME.md') -Destination (Join-Path $legalDestination 'Runtime.txt')
    $packagerLegal = Join-Path (Split-Path -Parent $jdkBin) 'legal\jdk.jpackage'
    if (Test-Path -LiteralPath $packagerLegal) {
        Copy-Item -LiteralPath $packagerLegal -Destination $legalDestination -Recurse
    }
    Write-Host "Built: $(Join-Path $destPath 'Passworder\Passworder.exe')"
} finally { Pop-Location }
