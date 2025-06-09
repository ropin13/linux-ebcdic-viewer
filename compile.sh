#!/bin/bash
# Script to compile the EBCDIC File Viewer

# Define the Lanterna JAR path (relative to project root)
# LANTERNA_JAR="lib/lanterna-3.1.1.jar" # Commented out as we now use lib/*
# Define source directory
SRC_DIR="src"
# Define output directory for compiled classes
OUT_DIR="out"

# Check if lib directory exists and contains JAR files
if [ ! -d "lib" ] || [ -z "$(ls -A lib/*.jar 2>/dev/null)" ]; then
    echo "Error: No JAR files found in 'lib' directory."
    echo "Please download Lanterna 3.1.1 and JRecord JARs and place them in the 'lib' directory."
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$OUT_DIR"

echo "Compiling Java source files from '$SRC_DIR' into '$OUT_DIR'..."
javac -Xlint:unchecked -cp "lib/*:$SRC_DIR" $SRC_DIR/*.java -d "$OUT_DIR"

if [ $? -eq 0 ]; then
    echo "Compilation successful. Class files are in '$OUT_DIR'."
else
    echo "Compilation failed."
    exit 1
fi
