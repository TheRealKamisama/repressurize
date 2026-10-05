param(
    [ValidatePattern('^Pressure(QA[12]|Visual|RevisionBefore|RevisionAfter)$')][string]$Username = 'PressureVisual',
    [ValidateSet('visual','network','revision')][string]$Mode = 'visual',
    [switch]$Baseline
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$installation = Join-Path $projectRoot 'build/package-client'
$instance = Join-Path $installation $Username
$libraryRoot = Join-Path $installation 'libraries'
$nativeRoot = Join-Path $instance 'natives'
$gradleRoot = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
$cacheRoot = Join-Path $gradleRoot 'caches'
$vanilla = Get-Content (Join-Path $cacheRoot 'forge_gradle/minecraft_repo/versions/1.20.1/version.json') -Raw | ConvertFrom-Json
$forge = Get-Content (Join-Path $installation 'versions/1.20.1-forge-47.1.39/1.20.1-forge-47.1.39.json') -Raw | ConvertFrom-Json
New-Item -ItemType Directory -Force $instance,$nativeRoot,(Join-Path $instance 'mods') | Out-Null
function Test-Rules($rules) {
    if (-not $rules) { return $true }
    $allowed = $false
    foreach ($rule in $rules) {
        if ($rule.os.name -and $rule.os.name -ne 'windows') { continue }
        if ($rule.os.arch -and $rule.os.arch -ne 'amd64') { continue }
        if ($rule.features) { continue }
        $allowed = $rule.action -eq 'allow'
    }
    return $allowed
}
$artifacts = [ordered]@{}
foreach ($library in @($vanilla.libraries) + @($forge.libraries)) {
    if ((Test-Rules $library.rules) -and $library.downloads.artifact) { $artifacts[$library.name] = $library.downloads.artifact }
}
$classPath = [Collections.Generic.List[string]]::new()
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($entry in $artifacts.GetEnumerator()) {
    $artifact = $entry.Value
    $destination = Join-Path $libraryRoot $artifact.path
    if (-not (Test-Path -LiteralPath $destination)) {
        New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
        $parts = $entry.Key.Split(':')
        $cacheDir = Join-Path $cacheRoot ('modules-2/files-2.1/' + $parts[0] + '/' + $parts[1] + '/' + $parts[2])
        $candidate = if (Test-Path -LiteralPath $cacheDir) { Get-ChildItem -LiteralPath $cacheDir -Recurse -File -Filter ([IO.Path]::GetFileName($destination)) | Select-Object -First 1 }
        if ($candidate -and ((Get-FileHash -Algorithm SHA1 -LiteralPath $candidate.FullName).Hash -eq $artifact.sha1)) {
            Copy-Item -LiteralPath $candidate.FullName -Destination $destination
        } else {
            & curl.exe -fsSL --proxy 'http://localhost:7897' --max-time 45 --retry 2 $artifact.url -o $destination
            if ($LASTEXITCODE -ne 0) { throw "Download failed: $($artifact.path)" }
        }
    }
    if ($artifact.sha1 -and (Get-FileHash -Algorithm SHA1 -LiteralPath $destination).Hash -ne $artifact.sha1) { throw "Checksum mismatch: $destination" }
    $classPath.Add($destination)
    if ($destination -match 'natives-windows\.jar$') {
        $archive = [IO.Compression.ZipFile]::OpenRead($destination)
        foreach ($file in $archive.Entries) { if ($file.Name.EndsWith('.dll')) {
            [IO.Compression.ZipFileExtensions]::ExtractToFile($file, (Join-Path $nativeRoot $file.Name), $true)
        } }
        $archive.Dispose()
    }
}
$clientJar = Join-Path $installation 'versions/1.20.1/1.20.1.jar'
if ((Get-FileHash -Algorithm SHA1 -LiteralPath $clientJar).Hash -ne $vanilla.downloads.client.sha1) { throw 'Minecraft client checksum mismatch' }
$classPath.Add($clientJar)
$digest = [Security.Cryptography.MD5]::HashData([Text.Encoding]::UTF8.GetBytes('OfflinePlayer:' + $Username))
$digest[6] = ($digest[6] -band 15) -bor 48
$digest[8] = ($digest[8] -band 63) -bor 128
$hex = [Convert]::ToHexString($digest).ToLowerInvariant()
$uuid = $hex.Substring(0,8)+'-'+$hex.Substring(8,4)+'-'+$hex.Substring(12,4)+'-'+$hex.Substring(16,4)+'-'+$hex.Substring(20)
$variables = @{
    auth_player_name=$Username; version_name='1.20.1-forge-47.1.39'; game_directory=$instance
    assets_root=(Join-Path $cacheRoot 'forge_gradle/assets'); assets_index_name=$vanilla.assetIndex.id
    auth_uuid=$uuid; auth_access_token='0'; clientid='0'; auth_xuid='0'; user_type='legacy'; version_type='release'
    natives_directory=$nativeRoot; launcher_name='PressurePackagedQA'; launcher_version='1'; library_directory=$libraryRoot
    classpath_separator=';'; classpath=($classPath -join ';')
}
function Expand-Argument([string]$argument) {
    foreach ($entry in $variables.GetEnumerator()) { $argument = $argument.Replace('${'+$entry.Key+'}', [string]$entry.Value) }
    if ($argument.Contains('${')) { throw "Unexpanded launcher argument: $argument" }
    return $argument.Replace('\','/')
}
function Get-Arguments($arguments) {
    foreach ($argument in $arguments) {
        if ($argument -is [string]) { Expand-Argument $argument }
        elseif (Test-Rules $argument.rules) { foreach ($value in $argument.value) { Expand-Argument $value } }
    }
}
$javaArgs = @('-Xms512M','-Xmx2G','-Dfile.encoding=UTF-8','-Dhttp.proxyHost=localhost','-Dhttp.proxyPort=7897','-Dhttps.proxyHost=localhost','-Dhttps.proxyPort=7897')
$javaArgs += if ($Mode -eq 'network') { '-Dpressure.networkQaClient=true' } elseif ($Mode -eq 'revision') { '-Dpressure.revisionQa=true' } else { '-Dpressure.migrationQa=true' }
if ($Mode -eq 'network') { $javaArgs += '-Dpressure.revisionNetwork=true' }
if ($Baseline) { $javaArgs += '-Dpressure.revisionBaseline=true' }
$javaArgs += Get-Arguments $vanilla.arguments.jvm
$javaArgs += Get-Arguments $forge.arguments.jvm
$javaArgs += '-DignoreList=bootstraplauncher,securejarhandler,asm-commons,asm-util,asm-analysis,asm-tree,asm,JarJarFileSystems,client-extra,fmlcore,javafmllanguage,lowcodelanguage,mclanguage,forge-,1.20.1.jar'
$javaArgs += $forge.mainClass
$javaArgs += Get-Arguments $vanilla.arguments.game
$javaArgs += Get-Arguments $forge.arguments.game
$javaArgs += if ($Mode -ne 'network') { @('--width','1280','--height','960') } else { @('--width','854','--height','480') }
$argFile = Join-Path $instance 'launch.args'
$javaArgs | ForEach-Object { '"'+$_.Replace('"','\"')+'"' } | Set-Content -LiteralPath $argFile -Encoding utf8NoBOM
Copy-Item -LiteralPath (Join-Path $projectRoot 'build/libs/pressure-2.0.0-DEV-mc1.20.1.jar') -Destination (Join-Path $instance 'mods')
if ($Baseline) {
    Copy-Item -LiteralPath (Join-Path $projectRoot 'build/migration-evidence/2026-10-05/baseline-pressure.jar') -Destination (Join-Path $instance 'mods/pressure-2.0.0-DEV-mc1.20.1.jar')
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'build/qa/pressure-qa.jar') -Destination (Join-Path $instance 'mods')
foreach ($dependency in @('bdlib-1.27.0.8-mc1.20.1.jar','scalablecatsforce-3.3.0-build-2-with-library.jar')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot ('build/package-server/mods/'+$dependency)) -Destination (Join-Path $instance 'mods')
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/options.txt') -Destination (Join-Path $instance 'options.txt')
if ($Mode -ne 'network' -and -not (Test-Path -LiteralPath (Join-Path $instance 'saves/Migration QA'))) {
    New-Item -ItemType Directory -Force (Join-Path $instance 'saves') | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'build/render-qa/saves/Migration QA') -Destination (Join-Path $instance 'saves/Migration QA') -Recurse
}
Write-Output $argFile
