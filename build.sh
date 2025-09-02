#!/bin/bash

# WhispWaypoints Build Script
# Provides easy version management and building

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Default values
INCREMENT_PATCH=false
INCREMENT_MINOR=false
INCREMENT_MAJOR=false
SET_MINECRAFT=""
SET_PLUGIN=""
BUILD=false
BUILD_PACK=false
CLEAN=false
SHOW_VERSION=false
HELP=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --increment-patch)
            INCREMENT_PATCH=true
            shift
            ;;
        --increment-minor)
            INCREMENT_MINOR=true
            shift
            ;;
        --increment-major)
            INCREMENT_MAJOR=true
            shift
            ;;
        --set-minecraft)
            SET_MINECRAFT="$2"
            shift 2
            ;;
        --set-plugin)
            SET_PLUGIN="$2"
            shift 2
            ;;
        --build)
            BUILD=true
            shift
            ;;
        --build-pack)
            BUILD_PACK=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --show-version)
            SHOW_VERSION=true
            shift
            ;;
        --help|-h)
            HELP=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Show help
if [ "$HELP" = true ]; then
    echo -e "${GREEN}WhispWaypoints Build Script${NC}"
    echo -e "${YELLOW}Usage: ./build.sh [options]${NC}"
    echo ""
    echo -e "${CYAN}Version Management:${NC}"
    echo "  --increment-patch      Increment plugin patch version (x.y.z -> x.y.z+1)"
    echo "  --increment-minor      Increment plugin minor version (x.y.z -> x.y+1.0)"
    echo "  --increment-major      Increment plugin major version (x.y.z -> x+1.0.0)"
    echo "  --set-minecraft x.y.z  Set Minecraft version"
    echo "  --set-plugin x.y.z     Set plugin version"
    echo "  --show-version         Display current version"
    echo ""
    echo -e "${CYAN}Build Commands:${NC}"
    echo "  --build                Build the plugin"
    echo "  --build-pack           Build plugin + resource pack"
    echo "  --clean                Clean and build"
    echo ""
    echo -e "${MAGENTA}Examples:${NC}"
    echo "  ./build.sh --increment-patch --build-pack # Increment patch and build everything"
    echo "  ./build.sh --set-minecraft 1.22.0         # Update Minecraft version"
    echo "  ./build.sh --set-plugin 2.0.0             # Set plugin to v2.0.0"
    exit 0
fi

# Function to show current version
show_version() {
    if [ -f "version.properties" ]; then
        # Read properties file (bash doesn't have native support, so we parse it)
        minecraft=$(grep "^minecraft=" version.properties | cut -d'=' -f2)
        plugin_major=$(grep "^plugin_major=" version.properties | cut -d'=' -f2)
        plugin_minor=$(grep "^plugin_minor=" version.properties | cut -d'=' -f2)
        plugin_patch=$(grep "^plugin_patch=" version.properties | cut -d'=' -f2)
        
        plugin="${plugin_major}.${plugin_minor}.${plugin_patch}"
        version="${minecraft}_${plugin}"
        
        echo -e "${GREEN}Current Version: $version${NC}"
        echo -e "${YELLOW}  Minecraft: $minecraft${NC}"
        echo -e "${YELLOW}  Plugin: $plugin${NC}"
    else
        echo -e "${RED}version.properties not found!${NC}"
    fi
}

# Show version if requested
if [ "$SHOW_VERSION" = true ]; then
    show_version
    exit 0
fi

# Version management operations
if [ -n "$SET_MINECRAFT" ]; then
    ./gradlew setMinecraftVersion -PmcVersion="$SET_MINECRAFT"
    echo -e "${GREEN}Set Minecraft version to $SET_MINECRAFT${NC}"
fi

if [ -n "$SET_PLUGIN" ]; then
    # Split version string by dots
    IFS='.' read -ra PARTS <<< "$SET_PLUGIN"
    if [ ${#PARTS[@]} -eq 3 ]; then
        cat > version.properties << EOF
# WhispWaypoints Version Configuration
# Format: MinecraftVersion_PluginVersion (e.g., 1.21.8_1.3.0)

# Minecraft version this plugin targets
minecraft=1.21.8

# Plugin version (major.minor.patch)
plugin_major=${PARTS[0]}
plugin_minor=${PARTS[1]}
plugin_patch=${PARTS[2]}
EOF
        echo -e "${GREEN}Plugin version set to $SET_PLUGIN${NC}"
    else
        echo -e "${RED}Invalid plugin version format. Use: x.y.z${NC}"
        exit 1
    fi
fi

# Increment version numbers
if [ "$INCREMENT_PATCH" = true ]; then
    ./gradlew incrementPatch
fi
if [ "$INCREMENT_MINOR" = true ]; then
    ./gradlew incrementMinor
fi
if [ "$INCREMENT_MAJOR" = true ]; then
    ./gradlew incrementMajor
fi

# Build the plugin
if [ "$CLEAN" = true ]; then
    echo -e "${CYAN}Cleaning and building...${NC}"
    ./gradlew clean build -x test
elif [ "$BUILD_PACK" = true ]; then
    echo -e "${CYAN}Building plugin and resource pack...${NC}"
    ./gradlew build -x test
    echo -e "${CYAN}Building resource pack...${NC}"
    ./build-resourcepack.sh
elif [ "$BUILD" = true ]; then
    echo -e "${CYAN}Building...${NC}"
    ./gradlew build -x test
fi

# Show final version
if [ "$INCREMENT_PATCH" = true ] || [ "$INCREMENT_MINOR" = true ] || [ "$INCREMENT_MAJOR" = true ] || \
   [ -n "$SET_MINECRAFT" ] || [ -n "$SET_PLUGIN" ] || [ "$BUILD" = true ] || \
   [ "$BUILD_PACK" = true ] || [ "$CLEAN" = true ]; then
    echo ""
    show_version
fi
