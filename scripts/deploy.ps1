param (
    [string]$Loader = "fabric",
    [string]$MultiMcModsDir = "D:\MultiMC\instances\26.3\.minecraft\mods",
    [string]$DownloadsDir = "$env:USERPROFILE\Downloads"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Deploying Block Place Mod [$Loader]" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$searchPath = if ($Loader -eq "neoforge") { "neoforge\build\libs" } else { "fabric\build\libs" }

if (-not (Test-Path $searchPath)) {
    Write-Error "Build output directory not found: $searchPath. Please build the mod first."
    exit 1
}

$jar = Get-ChildItem -Path $searchPath -Filter "blockplace-*.jar" | 
    Where-Object { $_.Name -notmatch "sources|javadoc" } | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 1

if (-not $jar) {
    Write-Error "No compiled mod JAR found in $searchPath. Run '.\gradlew.bat buildFabric' first."
    exit 1
}

$jarName = $jar.Name
Write-Host "[OK] Found Mod JAR: $jarName" -ForegroundColor Green

# 1. Deploy to Downloads
if (Test-Path $DownloadsDir) {
    Copy-Item -Path $jar.FullName -Destination $DownloadsDir -Force
    Write-Host "[OK] Copied to Downloads -> $DownloadsDir\$jarName" -ForegroundColor Green
} else {
    Write-Warning "Downloads directory not found: $DownloadsDir"
}

# 2. Deploy to MultiMC instance on D: drive
if (-not (Test-Path $MultiMcModsDir)) {
    New-Item -ItemType Directory -Path $MultiMcModsDir -Force | Out-Null
}
Copy-Item -Path $jar.FullName -Destination $MultiMcModsDir -Force
Write-Host "[OK] Copied to MultiMC  -> $MultiMcModsDir\$jarName" -ForegroundColor Green

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
