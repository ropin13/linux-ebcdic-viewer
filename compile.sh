#!/bin/bash
# Script to compile the EBCDIC File Viewer using Gradle

echo "Building the project with Gradle..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "Gradle build successful."
else
    echo "Gradle build failed."
    exit 1
fi
