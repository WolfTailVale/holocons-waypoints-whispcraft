# WhispWaypoints Resource Pack Builder
# Creates a zip file ready for distribution

Write-Host "Building WhispWaypoints Resource Pack..." -ForegroundColor Green

# Check if texture exists
$texturePath = "resourcepack\assets\minecraft\textures\item\whispwaypoints_token.png"
if (-not (Test-Path $texturePath)) {
    Write-Host "⚠️  Warning: Custom texture not found!" -ForegroundColor Yellow
    Write-Host "   Expected: $texturePath" -ForegroundColor Yellow
    Write-Host "   The pack will work but will use missing texture." -ForegroundColor Yellow
    Write-Host ""
}

# Create output directory
$outputDir = "build\resourcepack"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

# Get version for filename
$version = "unknown"
if (Test-Path "version.properties") {
    $props = Get-Content "version.properties" | ConvertFrom-StringData
    $version = "$($props.minecraft)_$($props.plugin_major).$($props.plugin_minor).$($props.plugin_patch)"
}

$packName = "WhispWaypoints-ResourcePack-$version.zip"
$outputPath = Join-Path $outputDir $packName

# Remove old pack if exists
if (Test-Path $outputPath) {
    Remove-Item $outputPath -Force
}

# Create zip file
try {
    $sourceDir = Join-Path $PWD "resourcepack"
    Compress-Archive -Path "$sourceDir\*" -DestinationPath $outputPath -CompressionLevel Optimal
    
    Write-Host "✅ Resource pack created successfully!" -ForegroundColor Green
    Write-Host "   File: $outputPath" -ForegroundColor Cyan
    Write-Host "   Size: $([math]::Round((Get-Item $outputPath).Length / 1KB, 1)) KB" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "📋 Next Steps:" -ForegroundColor Yellow
    Write-Host "   1. Add your custom texture: $texturePath" -ForegroundColor White
    Write-Host "   2. Test in Minecraft with the built pack" -ForegroundColor White
    Write-Host "   3. Upload to server for automatic distribution" -ForegroundColor White
    
}
catch {
    Write-Host "❌ Failed to create resource pack: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎨 Resource Pack Features:" -ForegroundColor Magenta
Write-Host "   • Custom model data: 1001" -ForegroundColor White
Write-Host "   • Pack format: 48 + supported_formats (MC 1.20.5+ through 1.21.x)" -ForegroundColor White
Write-Host "   • Auto-detects Waypoint Tokens" -ForegroundColor White
