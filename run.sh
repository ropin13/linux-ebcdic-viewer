#!/bin/bash
# Script to run the EBCDIC File Viewer from the JAR file

JAR_FILE="build/libs/linux-ebcdic-viewer.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found."
    echo "Please build the project first using compile.sh (e.g., 'bash compile.sh')."
    exit 1
fi

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <data_file_path> <copybook_file_path> [encoding] [page_size]"
    echo "For example: $0 data/test.dat data/test.cpy IBM037 50"
    echo "For Chinese instructions, please see README_zh.md (中文说明请参见 README_zh.md)"
    echo ""
    echo "To run with internally generated sample data (via AppController.main):"
    echo "java -cp \"lib/*:$OUT_DIR\" AppController"
    exit 1
fi

echo "Running EBCDIC File Viewer from $JAR_FILE..."
java -jar "$JAR_FILE" "$@"
