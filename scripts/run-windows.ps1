$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    # A source launch rebuilds the current checkout, including edits within one version.
    & .\mvnw.cmd -B -ntp -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Download the Windows ZIP from GitHub Releases if you do not have JDK 21.' }
    $javaw = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\javaw.exe' } else { (Get-Command javaw -ErrorAction Stop).Source }
    Start-Process -FilePath $javaw -ArgumentList @('-jar', ('"' + (Join-Path $projectRoot 'target\Passworder.jar') + '"'))
} finally { Pop-Location }
