# WhispWaypoints Build Script
# Provides easy version management and building

param(
    [switch]$IncrementPatch,
    [switch]$IncrementMinor,
    [switch]$IncrementMajor,
    [string]$SetMinecraft,
    [string]$SetPlugin,
    [switch]$Build,
    [switch]$Clean,
    [switch]$ShowVersion,
    [switch]$Help
)

# Show help
if ($Help) {
    Write-Host "WhispWaypoints Build Script" -ForegroundColor Green
    Write-Host "Usage: .\build.ps1 [options]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Version Management:" -ForegroundColor Cyan
    Write-Host "  -IncrementPatch      Increment plugin patch version (x.y.z -> x.y.z+1)"
    Write-Host "  -IncrementMinor      Increment plugin minor version (x.y.z -> x.y+1.0)"
    Write-Host "  -IncrementMajor      Increment plugin major version (x.y.z -> x+1.0.0)"
    Write-Host "  -SetMinecraft x.y.z  Set Minecraft version"
    Write-Host "  -SetPlugin x.y.z     Set plugin version"
    Write-Host "  -ShowVersion         Display current version"
    Write-Host ""
    Write-Host "Build Commands:" -ForegroundColor Cyan
    Write-Host "  -Build               Build the plugin"
    Write-Host "  -Clean               Clean and build"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Magenta
    Write-Host "  .\build.ps1 -IncrementPatch -Build     # Increment patch and compile"
    Write-Host "  .\build.ps1 -SetMinecraft 1.22.0       # Update Minecraft version"
    Write-Host "  .\build.ps1 -SetPlugin 2.0.0           # Set plugin to v2.0.0"
    exit
}

# Function to show current version
function Show-Version {
    if (Test-Path "version.properties") {
        $props = Get-Content "version.properties" | ConvertFrom-StringData
        $minecraft = $props.minecraft
        $plugin = "$($props.plugin_major).$($props.plugin_minor).$($props.plugin_patch)"
        $version = "${minecraft}_${plugin}"
        Write-Host "Current Version: $version" -ForegroundColor Green
        Write-Host "  Minecraft: $minecraft" -ForegroundColor Yellow
        Write-Host "  Plugin: $plugin" -ForegroundColor Yellow
    } else {
        Write-Host "version.properties not found!" -ForegroundColor Red
    }
}

# Show version if requested
if ($ShowVersion) {
    Show-Version
    exit
}

# Version management operations
if ($SetMinecraft) {
    .\gradlew.bat setMinecraftVersion -PmcVersion=$SetMinecraft
    Write-Host "Set Minecraft version to $SetMinecraft" -ForegroundColor Green
}

if ($SetPlugin) {
    $parts = $SetPlugin.Split('.')
    if ($parts.Length -eq 3) {
        $content = @"
# WhispWaypoints Version Configuration
# Format: MinecraftVersion_PluginVersion (e.g., 1.21.8_1.3.0)

# Minecraft version this plugin targets
minecraft=1.21.8

# Plugin version (major.minor.patch)
plugin_major=$($parts[0])
plugin_minor=$($parts[1])
plugin_patch=$($parts[2])
"@
        $content | Out-File -FilePath "version.properties" -Encoding UTF8
        Write-Host "Plugin version set to $SetPlugin" -ForegroundColor Green
    } else {
        Write-Host "Invalid plugin version format. Use: x.y.z" -ForegroundColor Red
        exit 1
    }
}

# Increment version numbers
if ($IncrementPatch) {
    .\gradlew.bat incrementPatch
}
if ($IncrementMinor) {
    .\gradlew.bat incrementMinor
}
if ($IncrementMajor) {
    .\gradlew.bat incrementMajor
}

# Build the plugin
if ($Clean) {
    Write-Host "Cleaning and building..." -ForegroundColor Cyan
    .\gradlew.bat clean build -x test
} elseif ($Build) {
    Write-Host "Building..." -ForegroundColor Cyan
    .\gradlew.bat build -x test
}

# Show final version
if ($IncrementPatch -or $IncrementMinor -or $IncrementMajor -or $SetMinecraft -or $SetPlugin -or $Build -or $Clean) {
    Write-Host ""
    Show-Version
}
