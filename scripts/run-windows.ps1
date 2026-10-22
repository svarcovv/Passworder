$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $version = ([xml](Get-Content -LiteralPath pom.xml -Raw)).project.version
    $candidates = @((Join-Path $projectRoot "dist\$version\Passworder"), (Join-Path $projectRoot 'dist\Passworder'))
    foreach ($candidate in $candidates) {
        $exe = Join-Path $candidate 'Passworder.exe'
        $cfg = Join-Path $candidate 'app\Passworder.cfg'
        if ((Test-Path -LiteralPath $exe) -and (Test-Path -LiteralPath $cfg)) {
            $configuration = Get-Content -LiteralPath $cfg -Raw
            if ($configuration -match ('(?m)^java-options=-Djpackage\.app-version=' + [regex]::Escape($version) + '\s*$')) {
                Start-Process -FilePath $exe
                exit 0
            }
        }
    }
    # A source checkout always builds current sources, never a stale JAR or older app image.
    & .\mvnw.cmd -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Download the Windows ZIP from GitHub Releases if you do not have JDK 21.' }
    $javaw = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\javaw.exe' } else { (Get-Command javaw -ErrorAction Stop).Source }
    Start-Process -FilePath $javaw -ArgumentList @('-jar', ('"' + (Join-Path $projectRoot 'target\Passworder.jar') + '"'))
} finally { Pop-Location }
