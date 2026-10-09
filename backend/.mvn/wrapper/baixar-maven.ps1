# Usado pelo mvnw.cmd: garante o Maven de maven-wrapper.properties em ~/.m2/wrapper/dists
# e imprime o caminho do mvn.cmd. Mensagens vão para stderr.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'  # a barra de progresso deixa o download muito lento no PowerShell 5
$props = Join-Path $PSScriptRoot 'maven-wrapper.properties'
$url = ((Get-Content $props | Where-Object { $_ -like 'distributionUrl=*' }) -replace '^distributionUrl=', '').Trim()
$nome = [IO.Path]::GetFileName($url) -replace '-bin\.zip$', ''
$dists = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
$mvn = Join-Path $dists "$nome\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    [Console]::Error.WriteLine("Baixando $nome...")
    New-Item -ItemType Directory -Force -Path $dists | Out-Null
    $zip = Join-Path $env:TEMP "$nome.zip"
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip
    Expand-Archive -Force -Path $zip -DestinationPath $dists
    Remove-Item $zip
}
Write-Output $mvn
