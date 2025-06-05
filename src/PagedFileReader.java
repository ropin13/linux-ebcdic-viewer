import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PagedFileReader {

    private String filePath;
    private List<CopybookLoader.FieldDefinition> fieldDefinitions;
    private int pageSize; // Number of records per page
    private String ebcdicEncoding;
    private RandomAccessFile randomAccessFile;
    private int recordLength;
    private long totalRecords;
    private int totalPages;
    private int currentPageNumber;
    private List<Map<String, String>> currentPageRawData; // Added to store current page

    public PagedFileReader(String filePath, List<CopybookLoader.FieldDefinition> fieldDefinitions, int pageSize, String ebcdicEncoding) throws IOException {
        this.filePath = filePath;
        this.currentPageRawData = new ArrayList<>(); // Initialize
        this.fieldDefinitions = fieldDefinitions;
        this.pageSize = pageSize;
        this.ebcdicEncoding = ebcdicEncoding;
        this.currentPageNumber = -1; // No page loaded initially

        if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Field definitions cannot be null or empty.");
        }

        this.recordLength = calculateRecordLength();

        try {
            this.randomAccessFile = new RandomAccessFile(filePath, "r"); // "r" for read-only
            long fileLength = this.randomAccessFile.length();
            if (this.recordLength == 0 && fileLength > 0) {
                 throw new IllegalArgumentException("Record length is zero, cannot process non-empty file.");
            } else if (this.recordLength == 0 && fileLength == 0) {
                this.totalRecords = 0;
                this.totalPages = 0;
            } else if (fileLength % this.recordLength != 0) {
                System.err.println("Warning: File length " + fileLength + " is not an exact multiple of record length " + this.recordLength);
                // Potentially handle this as an error or allow partial last record processing if needed
                this.totalRecords = fileLength / this.recordLength; // Integer division gives complete records
            } else {
                this.totalRecords = fileLength / this.recordLength;
            }

            if (this.totalRecords == 0) {
                this.totalPages = 0;
            } else {
                this.totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            }

        } catch (IOException e) {
            System.err.println("Error opening or reading file: " + filePath + " - " + e.getMessage());
            throw e; // Re-throw to inform the caller
        }
    }

    private int calculateRecordLength() {
        int length = 0;
        if (fieldDefinitions != null) {
            for (CopybookLoader.FieldDefinition field : fieldDefinitions) {
                length += field.getLength();
            }
        }
        return length;
    }

    public List<Map<String, String>> getPage(int pageNumber) throws IOException {
        if (pageNumber < 0 || pageNumber >= totalPages) {
            // Or throw IllegalArgumentException
            System.err.println("Invalid page number: " + pageNumber + ". Total pages: " + totalPages);
            return new ArrayList<>(); // Return empty list for invalid page
        }
        if (recordLength == 0 && totalRecords == 0) { // Handle empty file
            return new ArrayList<>();
        }


        List<Map<String, String>> pageData = new ArrayList<>();
        long startBytePosition = (long) pageNumber * pageSize * recordLength;
        randomAccessFile.seek(startBytePosition);

        byte[] recordBuffer = new byte[recordLength];
        int recordsToRead = pageSize;
        if (pageNumber == totalPages - 1) { // Last page might have fewer records
            recordsToRead = (int) (totalRecords - ((long)pageNumber * pageSize));
        }


        for (int i = 0; i < recordsToRead; i++) {
            int bytesRead = randomAccessFile.read(recordBuffer);
            if (bytesRead == -1) { // End of file reached unexpectedly
                break;
            }
            if (bytesRead < recordLength) {
                System.err.println("Warning: Incomplete record read at record index " + (pageNumber * pageSize + i) + ". Expected " + recordLength + " bytes, got " + bytesRead);
                // Optionally, pad or skip this record
                continue;
            }

            Map<String, String> recordMap = new HashMap<>();
            try {
                for (CopybookLoader.FieldDefinition field : fieldDefinitions) {
                    // Field start positions are 1-based in copybook, adjust to 0-based for array
                    int fieldStartInRecord = field.getStartPosition() - 1;
                    int fieldLength = field.getLength();

                    if (fieldStartInRecord + fieldLength > recordBuffer.length) {
                         System.err.println("Error: Field " + field.getName() + " definition exceeds record buffer length. Skipping field.");
                         recordMap.put(field.getName(), "ERROR_FIELD_OUT_OF_BOUNDS");
                         continue;
                    }

                    byte[] fieldBytes = new byte[fieldLength];
                    System.arraycopy(recordBuffer, fieldStartInRecord, fieldBytes, 0, fieldLength);
                    String decodedString = new String(fieldBytes, ebcdicEncoding);
                    // Sanitize the string: replace control characters (0x00-0x1F, 0x7F-0x9F) with a placeholder like '.'
                    // This regex matches most C0 and C1 control characters.
                    // For simplicity, replacing with "." If specific behavior for specific control chars is needed,
                    // this logic would be more complex.
                    String sanitizedString = decodedString.replaceAll("\\p{Cntrl}", ".");
                    recordMap.put(field.getName(), sanitizedString);
                }
            } catch (UnsupportedEncodingException e) {
                // 'field' is not in scope here.
                System.err.println("Error decoding EBCDIC field during record processing: " + e.getMessage());
                // Put a placeholder or handle error as required
                // To associate with a specific field, the try-catch would need to be inside the loop.
                // For now, a general error for the record is placed.
                recordMap.put("RECORD_DECODING_ERROR", "A field in this record failed EBCDIC decoding: " + e.getMessage());
            }
            pageData.add(recordMap);
        }
        this.currentPageNumber = pageNumber;
        this.currentPageRawData = pageData; // Store the loaded page data
        return pageData;
    }

    /**
     * Returns the raw data of the most recently loaded page.
     * This does not re-read from the file but returns the in-memory copy.
     * @return List of records for the current page.
     */
    public List<Map<String, String>> getCurrentPageRawData() {
        return this.currentPageRawData;
    }

    public List<Map<String, String>> nextPage() throws IOException {
        if (currentPageNumber < totalPages - 1) {
            return getPage(currentPageNumber + 1);
        }
        return null; // Or throw exception, or return current page if already at last
    }

    public List<Map<String, String>> previousPage() throws IOException {
        if (currentPageNumber > 0) {
            return getPage(currentPageNumber - 1);
        }
        return null; // Or throw exception, or return current page if already at first
    }

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public void close() throws IOException {
        if (randomAccessFile != null) {
            randomAccessFile.close();
        }
    }

    // Main method for basic testing (requires a sample EBCDIC file and copybook)
    public static void main(String[] args) {
        // This is a placeholder for testing.
        // Actual testing requires a .cpy file and a corresponding EBCDIC data file.
        System.out.println("PagedFileReader.java compiled. To test, create a sample EBCDIC data file and a copybook.");
        System.out.println("Then, instantiate CopybookLoader, load definitions, then instantiate PagedFileReader.");
        System.out.println("Example (pseudo-code):");
        System.out.println("  CopybookLoader cbl = new CopybookLoader(\"test.cpy\");");
        System.out.println("  List<FieldDefinition> defs = cbl.getFieldDefinitions();");
        System.out.println("  if(defs.isEmpty()) { System.out.println(\"No defs in copybook\"); return; }");
        System.out.println("  try {");
        System.out.println("    PagedFileReader pfr = new PagedFileReader(\"test.dat\", defs, 10, \"CP037\");");
        System.out.println("    System.out.println(\"Total records: \" + pfr.getTotalRecords());");
        System.out.println("    System.out.println(\"Total pages: \" + pfr.getTotalPages());");
        System.out.println("    List<Map<String, String>> page0 = pfr.getPage(0);");
        System.out.println("    // Print page0 data");
        System.out.println("    pfr.close();");
        System.out.println("  } catch (IOException e) { e.printStackTrace(); }");


        // Create dummy files for testing if they don't exist
        String copybookFilePath = "test.cpy";
        String dataFilePath = "test.dat";

        try {
            java.io.File cpyFile = new java.io.File(copybookFilePath);
            if (!cpyFile.exists()) {
                System.out.println("\nCreating dummy " + copybookFilePath + " for basic compilation test...");
                try (java.io.PrintWriter writer = new java.io.PrintWriter(copybookFilePath)) {
                    writer.println("01 RECORD-LAYOUT.");
                    writer.println("  05 NAME PIC X(10).");
                    writer.println("  05 VALUE PIC X(5).");
                }
            }

            java.io.File datFile = new java.io.File(dataFilePath);
            if (!datFile.exists()) {
                 System.out.println("Creating dummy " + dataFilePath + " for basic compilation test (will be empty or have minimal data)...");
                // For a real test, this file should contain EBCDIC encoded data
                // For this dummy test, we can leave it empty or add some sample bytes.
                // Adding simple ASCII for placeholder, won't decode correctly but allows file operations.
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dataFilePath)) {
                    // Record 1: NAME="AAAAAAAAAA", VALUE="BBBBB" (15 bytes)
                    // Record 2: NAME="CCCCCCCCCC", VALUE="DDDDD" (15 bytes)
                    // This is NOT EBCDIC. CP037 would be different byte values.
                    fos.write("AAAAAAAAAABBBBB".getBytes("ASCII"));
                    fos.write("CCCCCCCCCCDDDDD".getBytes("ASCII"));
                }
            }
            System.out.println("\n--- Running Test with Dummy Files ---");
            CopybookLoader cbl = new CopybookLoader(copybookFilePath);
            List<CopybookLoader.FieldDefinition> defs = cbl.getFieldDefinitions();
            if(defs.isEmpty()){
                System.out.println("Copybook parsing failed or is empty. Check " + copybookFilePath);
                return;
            }
            System.out.println("Record length from copybook: " + cbl.getFieldDefinitions().stream().mapToInt(CopybookLoader.FieldDefinition::getLength).sum());


            PagedFileReader pfr = new PagedFileReader(dataFilePath, defs, 1, "CP037"); // Page size 1 for easier testing
            System.out.println("Total records: " + pfr.getTotalRecords());
            System.out.println("Total pages: " + pfr.getTotalPages());
            System.out.println("Calculated Record Length: " + pfr.getRecordLength());


            if (pfr.getTotalPages() > 0) {
                List<Map<String, String>> page0 = pfr.getPage(0);
                System.out.println("\nPage 0 Data (first record if exists):");
                if (!page0.isEmpty()) {
                    System.out.println(page0.get(0));
                } else {
                    System.out.println("Page 0 is empty.");
                }
                 if (pfr.getTotalPages() > 1) {
                    List<Map<String, String>> page1 = pfr.nextPage();
                     System.out.println("\nPage 1 Data (first record if exists):");
                    if (page1 != null && !page1.isEmpty()) {
                         System.out.println(page1.get(0));
                    } else {
                        System.out.println("Page 1 is empty or could not be loaded.");
                    }
                }


            } else {
                System.out.println("No pages to display as totalPages is 0.");
            }
            pfr.close();
            System.out.println("\n--- Test with Dummy Files Finished ---");


        } catch (IOException e) {
            System.err.println("Error during dummy file test setup or PagedFileReader execution: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
