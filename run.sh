#!/bin/bash
# Script to run the EBCDIC File Viewer

# Define output directory for compiled classes
OUT_DIR="out"

# Check if lib directory exists and contains JAR files (as they are needed for runtime)
if [ ! -d "lib" ] || [ -z "$(ls -A lib/*.jar 2>/dev/null)" ]; then
    echo "Error: No JAR files found in 'lib' directory."
    echo "Please ensure Lanterna 3.1.1 and JRecord JARs are in the 'lib' directory."
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
    echo "For Chinese instructions, please see README_zh.md (中文说明请参见 README_zh.md)"
    echo ""
    echo "To run with internally generated sample data (via AppController.main):"
    echo "java -cp \"lib/*:$OUT_DIR\" AppController"
    exit 1
fi

echo "Running EBCDIC File Viewer..."
# Pass all script arguments to the Java application
# Quoting "$@" ensures that arguments with spaces are passed correctly
java -cp "lib/*:$OUT_DIR" EbcdicFileViewer "$@"
