param([string]$AppImage, [string]$Destination = 'dist\installer')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $AppImage) { throw 'Specify -AppImage with a completed Windows application image.' }
$image = (Resolve-Path -LiteralPath $AppImage).Path
$version = ([xml](Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw)).project.version
$configuration = Get-Content -LiteralPath (Join-Path $image 'app\Passworder.cfg') -Raw
if ($configuration -notmatch ('(?m)^java-options=-Djpackage\.app-version=' + [regex]::Escape($version) + '\s*$')) { throw 'Application image version does not match pom.xml.' }
$compiler = Join-Path $env:SystemRoot 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$work = Join-Path $projectRoot 'target\installer'
New-Item -ItemType Directory -Path $work -Force | Out-Null
$output = [IO.Path]::GetFullPath((Join-Path $projectRoot $Destination))
New-Item -ItemType Directory -Path $output -Force | Out-Null
$source = Join-Path $projectRoot 'installer\Setup.cs'
$icon = Join-Path $projectRoot 'src\main\resources\icons\passworder.ico'
$manifest = Join-Path $projectRoot 'installer\app.manifest'
$attributes = Join-Path $work 'VersionInfo.cs'
[IO.File]::WriteAllText($attributes, ('using System.Reflection; [assembly: AssemblyProduct("Passworder")] [assembly: AssemblyCompany("svarcovv")] [assembly: AssemblyFileVersion("' + $version + '.0")] [assembly: AssemblyVersion("' + $version + '.0")]'), [Text.UTF8Encoding]::new($false))
$common = @('/nologo', '/target:winexe', '/optimize+', '/platform:anycpu', '/codepage:65001', ('/win32icon:' + $icon), ('/win32manifest:' + $manifest), '/reference:System.Windows.Forms.dll', '/reference:System.Drawing.dll', '/reference:System.IO.Compression.dll', '/reference:System.IO.Compression.FileSystem.dll', '/reference:Microsoft.CSharp.dll')
& $compiler @common ('/out:' + (Join-Path $image 'Uninstall.exe')) $source $attributes
if ($LASTEXITCODE -ne 0) { throw 'Uninstaller compilation failed.' }
$payload = Join-Path $work 'payload.zip'
if (Test-Path -LiteralPath $payload) { Remove-Item -LiteralPath $payload }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($image, $payload, [IO.Compression.CompressionLevel]::Optimal, $false)
$metadata = Join-Path $work 'build'
$jarHash = (Get-FileHash -LiteralPath (Join-Path $image 'app\Passworder.jar') -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText($metadata, "$version`n$jarHash", [Text.UTF8Encoding]::new($false))
$setup = Join-Path $output 'Passworder-Setup.exe'
& $compiler @common ('/resource:' + $payload + ',Passworder.Payload') ('/resource:' + $metadata + ',Passworder.Build') ('/out:' + $setup) $source $attributes
if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed.' }
Write-Output "Built: $setup"
