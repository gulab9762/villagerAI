param (
    [string]$Loader = "fabric",
    [string]$MultiMcModsDir = "D:\MultiMC\instances\26.3\.minecraft\mods",
    [string]$DownloadsDir = "$env:USERPROFILE\Downloads"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Deploying Heavenly Nether Mod [$Loader]" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$searchPath = if ($Loader -eq "neoforge") { "neoforge\build\libs" } else { "fabric\build\libs" }

if (-not (Test-Path $searchPath)) {
    Write-Error "Build output directory not found: $searchPath. Please build the mod first."
    exit 1
}

$jar = Get-ChildItem -Path $searchPath -Filter "heavenlynether-*.jar" | 
    Where-Object { $_.Name -notmatch "sources|javadoc" } | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 1

if (-not $jar) {
    Write-Error "No compiled mod JAR found in $searchPath. Run '.\gradlew.bat buildFabric' first."
    exit 1
}

$jarName = $jar.Name
Write-Host "[OK] Found Mod JAR: $jarName" -ForegroundColor Green

# 1. Clean previous old mod jars from target directories
if (Test-Path $DownloadsDir) {
    Get-ChildItem -Path $DownloadsDir -Filter "blockplace-*.jar" | Remove-Item -Force -ErrorAction SilentlyContinue
    Copy-Item -Path $jar.FullName -Destination $DownloadsDir -Force
    Write-Host "[OK] Copied to Downloads -> $DownloadsDir\$jarName" -ForegroundColor Green
}

if (-not (Test-Path $MultiMcModsDir)) {
    New-Item -ItemType Directory -Path $MultiMcModsDir -Force | Out-Null
}
Get-ChildItem -Path $MultiMcModsDir -Filter "blockplace-*.jar" | Remove-Item -Force -ErrorAction SilentlyContinue
Copy-Item -Path $jar.FullName -Destination $MultiMcModsDir -Force
Write-Host "[OK] Copied to MultiMC  -> $MultiMcModsDir\$jarName" -ForegroundColor Green

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
