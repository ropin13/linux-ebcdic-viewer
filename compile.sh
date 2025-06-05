#!/bin/bash
# Script to compile the EBCDIC File Viewer

# Define the Lanterna JAR path (relative to project root)
LANTERNA_JAR="lib/lanterna-3.1.1.jar"
# Define source directory
SRC_DIR="src"
# Define output directory for compiled classes
OUT_DIR="out"

# Check if Lanterna JAR exists
if [ ! -f "$LANTERNA_JAR" ]; then
    echo "Error: Lanterna JAR not found at $LANTERNA_JAR"
    echo "Please download Lanterna 3.1.1 and place it in the 'lib' directory."
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$OUT_DIR"

echo "Compiling Java source files from '$SRC_DIR' into '$OUT_DIR'..."
javac -Xlint:unchecked -cp "$LANTERNA_JAR:$SRC_DIR" $SRC_DIR/*.java -d "$OUT_DIR"

if [ $? -eq 0 ]; then
    echo "Compilation successful. Class files are in '$OUT_DIR'."
else
    echo "Compilation failed."
    exit 1
fi
