param([string]$LegacyImage, [string]$NewImage, [string]$Installer)
$ErrorActionPreference='Stop'
$testRoot=Join-Path $PSScriptRoot ('..\work\install-smoke-'+[guid]::NewGuid().ToString('N'))
$testRoot=[IO.Path]::GetFullPath($testRoot)
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
$profile=Join-Path $testRoot 'profile'
$data=Join-Path $profile 'Passworder'
$installed=Join-Path $testRoot 'installed'
New-Item -ItemType Directory -Path $data -Force | Out-Null
$vault=Join-Path $data 'vault.pwr'
$backup=Join-Path $data 'vault.pwr.bak'
[IO.File]::WriteAllBytes($vault,[Text.Encoding]::UTF8.GetBytes('Synthetic encrypted vault sentinel.'))
[IO.File]::WriteAllBytes($backup,[Text.Encoding]::UTF8.GetBytes('Synthetic backup sentinel.'))
[IO.File]::WriteAllText((Join-Path $data 'settings.properties'),"hotkey=Ctrl+Shift+P`ntheme=dark`n")
$vaultHash=(Get-FileHash -LiteralPath $vault).Hash
$backupHash=(Get-FileHash -LiteralPath $backup).Hash
$started=@()
function Start-TestProcess([string]$File,[string]$Arguments=''){
$info=[Diagnostics.ProcessStartInfo]::new((Resolve-Path -LiteralPath $File).Path,$Arguments)
$info.UseShellExecute=$false
$info.CreateNoWindow=$true
$info.Environment['LOCALAPPDATA']=$profile
$process=[Diagnostics.Process]::Start($info)
$script:started+= $process
return $process
}
function Wait-For([scriptblock]$Condition,[string]$Message,[int]$Seconds=30){
$deadline=[DateTime]::UtcNow.AddSeconds($Seconds)
while([DateTime]::UtcNow -lt $deadline){if(& $Condition){return};Start-Sleep -Milliseconds 150}
throw "Timed out: $Message"
}
function Running-Version {
$file=Join-Path $data 'running.version'
if(Test-Path -LiteralPath $file){return Get-Content -LiteralPath $file -Raw}
return ''
}
function Assert-Vault {
if((Get-FileHash -LiteralPath $vault).Hash -ne $vaultHash -or (Get-FileHash -LiteralPath $backup).Hash -ne $backupHash){throw 'Vault or backup changed.'}
}
try {
$old=Start-TestProcess (Join-Path $LegacyImage 'Passworder.exe')
Wait-For { (Running-Version).Trim() -eq '0.2.2' } 'legacy startup'
$args='--quiet --no-shortcuts --no-registry --root="'+$installed+'"'
$setup=Start-TestProcess $Installer $args
if(-not $setup.WaitForExit(60000) -or $setup.ExitCode -ne 0){throw "Initial installation failed: $($setup.ExitCode)"}
Wait-For { (Running-Version).Trim() -eq '0.2.3' } 'installed version startup'
Wait-For {$old.HasExited} 'legacy process exit'
Assert-Vault
Write-Output 'PASS: installing over a running legacy release opens the downloaded build.'
$copy=Join-Path $testRoot 'downloaded'
Copy-Item -LiteralPath $NewImage -Destination $copy -Recurse
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar=[IO.Compression.ZipFile]::Open((Join-Path $copy 'app\Passworder.jar'),[IO.Compression.ZipArchiveMode]::Update)
try{$entry=$jar.CreateEntry('synthetic-build-marker');$writer=[IO.StreamWriter]::new($entry.Open());$writer.Write('Different build with the same version.');$writer.Dispose()}finally{$jar.Dispose()}
$expected=(Get-FileHash -LiteralPath (Join-Path $copy 'app\Passworder.jar')).Hash.ToLowerInvariant()
$replacement=Start-TestProcess (Join-Path $copy 'Passworder.exe')
Wait-For { $file=Join-Path $data 'running.properties'; (Test-Path -LiteralPath $file) -and (Get-Content -LiteralPath $file -Raw).Contains('build='+$expected) } 'different build takeover'
Assert-Vault
Write-Output 'PASS: a downloaded build replaces the running build even when the version number matches.'
$again=Start-TestProcess $Installer $args
if(-not $again.WaitForExit(60000) -or $again.ExitCode -ne 0){throw 'Repeat installation failed.'}
$original=(Get-FileHash -LiteralPath (Join-Path $NewImage 'app\Passworder.jar')).Hash.ToLowerInvariant()
Wait-For { $file=Join-Path $data 'running.properties'; (Test-Path -LiteralPath $file) -and (Get-Content -LiteralPath $file -Raw).Contains('build='+$original) } 'installer replaces running portable build'
Assert-Vault
Write-Output 'PASS: repeating installation refreshes the same application directory.'
$preparer=Start-TestProcess (Join-Path $NewImage 'runtime\bin\java.exe') ('-cp "'+(Join-Path (Resolve-Path -LiteralPath $NewImage).Path 'app\Passworder.jar')+'" svarcovv.dev.passworder.Passworder --prepare-update')
if(-not $preparer.WaitForExit(30000) -or $preparer.ExitCode -ne 0){throw 'Could not prepare editor fixture.'}
$ready=Join-Path $testRoot 'editor-ready'
$release=Join-Path $testRoot 'editor-release'
$classPath=(Join-Path $PSScriptRoot '..\target\test-classes')+';'+(Join-Path (Resolve-Path -LiteralPath $NewImage).Path 'app\Passworder.jar')
$editor=Start-TestProcess (Join-Path $NewImage 'runtime\bin\java.exe') ('-cp "'+$classPath+'" svarcovv.dev.passworder.ui.UpgradeFixture "'+$data+'" "'+$ready+'" "'+$release+'"')
Wait-For {Test-Path -LiteralPath $ready} 'editor fixture ready'
$waiting=Start-TestProcess (Join-Path $installed 'Passworder.exe')
Start-Sleep -Milliseconds 1200
if($editor.HasExited -or -not (Get-Content -LiteralPath (Join-Path $data 'running.properties') -Raw).Contains('build=editor-fixture')){throw 'Handover discarded an open editor.'}
[IO.File]::WriteAllText($release,'continue')
Wait-For {$editor.HasExited} 'handover after editor closes'
Wait-For {(Get-Content -LiteralPath (Join-Path $data 'running.properties') -Raw).Contains('build='+$original)} 'downloaded app starts after editing'
Assert-Vault
Write-Output 'PASS: an open editor blocks replacement until it is closed.'
$uninstall=Start-TestProcess (Join-Path $installed 'Uninstall.exe') ('--uninstall --quiet --root="'+$installed+'"')
Wait-For { -not (Test-Path -LiteralPath $installed) } 'uninstall' 60
Assert-Vault
Write-Output 'PASS: uninstall closes the app and preserves the vault and backup.'
$invalid=Start-TestProcess $Installer ('--quiet --no-shortcuts --no-registry --root="'+$data+'"')
if(-not $invalid.WaitForExit(30000) -or $invalid.ExitCode -eq 0){throw 'Installer accepted the vault directory as its target.'}
Assert-Vault
Write-Output 'PASS: the vault directory cannot be used as an installation target.'
} finally {
foreach($process in $started){if(-not $process.HasExited){$own=Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)";if($own.ExecutablePath -and $own.ExecutablePath.StartsWith($testRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue}};$process.Dispose()}
$remaining=@(Get-CimInstance Win32_Process -Filter "Name = 'Passworder.exe'" | Where-Object {$_.ExecutablePath -and $_.ExecutablePath.StartsWith($testRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)})
foreach($process in $remaining){Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue}
}
