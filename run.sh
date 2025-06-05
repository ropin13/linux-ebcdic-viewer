#!/bin/bash
# Script to run the EBCDIC File Viewer

# Define the Lanterna JAR path
LANTERNA_JAR="lib/lanterna-3.1.1.jar"
# Define output directory for compiled classes
OUT_DIR="out"

# Check if Lanterna JAR exists (as it's needed for runtime)
if [ ! -f "$LANTERNA_JAR" ]; then
    echo "Error: Lanterna JAR not found at $LANTERNA_JAR"
    echo "Please ensure Lanterna 3.1.1 is in the 'lib' directory."
    exit 1
fi

# Check if compiled classes exist in the output directory
if [ ! -d "$OUT_DIR" ] || [ -z "$(ls -A $OUT_DIR)" ]; then
    echo "Error: Compiled classes not found in '$OUT_DIR'."
    echo "Please compile the project first using compile.sh (e.g., 'bash compile.sh')."
    exit 1
fi

# Check if at least data file and copybook file are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <data_file_path> <copybook_file_path> [encoding] [page_size]"
    echo "For example: $0 data/test.dat data/test.cpy IBM037 50"
    echo ""
    echo "To run with internally generated sample data (via AppController.main):"
    echo "java -cp \"$LANTERNA_JAR:$OUT_DIR\" AppController"
    exit 1
fi

echo "Running EBCDIC File Viewer..."
# Pass all script arguments to the Java application
# Quoting "$@" ensures that arguments with spaces are passed correctly
java -cp "$LANTERNA_JAR:$OUT_DIR" EbcdicFileViewer "$@"
