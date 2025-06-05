# EBCDIC File Viewer

## Overview
The EBCDIC File Viewer is a Java-based command-line utility designed to display EBCDIC (Extended Binary Coded Decimal Interchange Code) encoded data files in a human-readable format. It uses a COBOL copybook to interpret the record structure within the EBCDIC file, allowing users to view field names and their corresponding values. The tool provides a text-based user interface (TUI) for navigation and basic data interaction.

This tool is particularly useful for developers and data analysts who need to inspect mainframe data files or other EBCDIC-encoded data sources without requiring specialized mainframe access or tools.

## Features
- **EBCDIC Data Parsing**: Reads and interprets EBCDIC encoded files.
- **Copybook-based Field Definition**: Uses a COBOL copybook to define the structure of records in the data file.
- **Text-Based User Interface (TUI)**: Provides an interactive terminal interface for viewing data.
- **Paged Data Browsing**: Displays data in pages for manageable viewing of large files.
    - Navigate to next/previous pages.
- **Field-based Search**: Allows searching for specific text within a field on the current page.
- **Customizable Encoding**: Supports specifying different EBCDIC encodings.
- **Customizable Page Size**: Allows users to define the number of records displayed per page.
- **Control Character Sanitization**: Replaces non-printable EBCDIC control characters to prevent display issues.

## Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher.
- **Lanterna 3.1.1 Library**: The `lanterna-3.1.1.jar` file is required. It should be placed in the `lib` directory in the project root. The `compile.sh` script will check for this file. If it's missing, you can usually download it from Maven Central or other repositories (search for "lanterna-3.1.1.jar").

## Directory Structure
```
.
├── lib/
│   └── lanterna-3.1.1.jar  # Lanterna library
├── src/
│   ├── AppController.java
│   ├── CopybookLoader.java
│   ├── EbcdicFileViewer.java # Main entry point
│   ├── PagedFileReader.java
│   ├── SearchManager.java
│   └── TUIView.java
├── out/                      # Default directory for compiled .class files (created by compile.sh)
├── data/                     # Optional: Can be used to store data/copybook files.
│                             # AppController.main() creates sample files here if run without args.
├── README.md                 # This file
├── compile.sh                # Compilation script
└── run.sh                    # Execution script
```

## Compilation
The project includes a `compile.sh` script to simplify compilation. Ensure it has execute permissions (`chmod +x compile.sh`).

To compile:
```bash
bash compile.sh
```
This script will place the compiled `.class` files into the `out/` directory.

Alternatively, you can compile manually (ensure `out/` directory exists):
```bash
javac -cp "lib/lanterna-3.1.1.jar:src" src/*.java -d out
```

## Execution
The project includes a `run.sh` script for easier execution. Ensure it has execute permissions (`chmod +x run.sh`).

To run the application:
```bash
bash run.sh <data_file_path> <copybook_file_path> [encoding] [page_size]
```
Follow the usage instructions printed by the script if arguments are missing.

Alternatively, you can run manually after compilation:
```bash
java -cp "lib/lanterna-3.1.1.jar:out" EbcdicFileViewer <data_file_path> <copybook_file_path> [encoding] [page_size]
```
(If you placed your classes into a package, e.g., `com.viewer`, the command would be `java -cp "lib/lanterna-3.1.1.jar:out" com.viewer.EbcdicFileViewer ...`)

### Command-line Arguments
-   `<data_file_path>`: Path to the EBCDIC data file (required).
-   `<copybook_file_path>`: Path to the COBOL copybook file (required).
-   `[encoding]`: EBCDIC encoding to use (optional, defaults to "IBM037"). Common examples: `CP037`, `IBM500`, `IBM1047`.
-   `[page_size]`: Number of records per page (optional, defaults to 50). Must be a positive integer.

## Sample Usage (with Internally Generated Data)
The `AppController` class contains a `main` method that can generate and use sample EBCDIC data and a sample copybook if run without arguments. This is useful for a quick test of the application's UI and core functionality without needing external files.

To run with sample data (after compiling):
```bash
java -cp "lib/lanterna-3.1.1.jar:out" AppController
```
This will create `data/test.dat` and `data/test.cpy` if they don't exist and launch the viewer with this data.

## Performance Notes
- **Memory**: The application loads one page of data into memory at a time. The size of a "page" is determined by the number of records per page (user-defined) and the length of each record (defined by the copybook). For very large record lengths or very high page sizes, memory usage can increase.
- **Speed**:
    - File reading is done using `RandomAccessFile` for efficient seeking to page boundaries.
    - EBCDIC to String conversion and subsequent sanitization happens per field for each record on the displayed page.
    - Search operations are performed on the currently loaded page data only and are case-insensitive.
- **Large Files**: The tool is designed to handle large files by only processing data page by page. The initial calculation of total records requires reading the file size, which is generally fast.
- **TUI Rendering**: Lanterna is generally efficient for TUI rendering. Performance may vary depending on the terminal emulator and system environment.
```
