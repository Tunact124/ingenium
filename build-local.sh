#!/bin/bash

# Ingenium Optimization Mod - Local Build Script
# This script builds the mod on your local machine

set -e

echo "======================================"
echo "Ingenium Optimization Mod Builder"
echo "======================================"
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please install Java 17 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "Found Java version: $JAVA_VERSION"

# Make gradlew executable
chmod +x gradlew

# Clean previous builds
echo ""
echo "Cleaning previous builds..."
./gradlew clean

# Build the mod
echo ""
echo "Building Ingenium Optimization Mod..."
./gradlew build

# Check if build succeeded
if [ -f "build/libs/Ingenium-optimization-*.jar" ]; then
    echo ""
    echo "======================================"
    echo "Build Successful!"
    echo "======================================"
    echo ""
    echo "Mod JAR location:"
    ls -lh build/libs/Ingenium-optimization-*.jar
    echo ""
    echo "To install:"
    echo "1. Copy the JAR to your Minecraft mods folder"
    echo "2. Ensure you have Fabric Loader installed"
    echo "3. Launch Minecraft and enjoy the optimizations!"
else
    echo ""
    echo "Build may have failed. Check the output above for errors."
    exit 1
fi
