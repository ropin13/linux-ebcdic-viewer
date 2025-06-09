import java.io.IOException;
//import java.io.RandomAccessFile; // Removed
//import java.io.UnsupportedEncodingException; // Removed, JRecord handles
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// JRecord imports
import net.sf.JRecord.JRecordInterface1;
import net.sf.JRecord.Common.Constants;
import net.sf.JRecord.Common.Conversion;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.Details.LayoutDetail;
import net.sf.JRecord.External.CopybookLoader; // Still used in main for now, but not by PagedFileReader core
import net.sf.JRecord.External.ExternalRecord;
import net.sf.JRecord.External.Def.ExternalField;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.IO.CobolIoProvider;
import net.sf.JRecord.Numeric.Convert; // For FMT_MAINFRAME etc.


public class PagedFileReader {

    private String filePath;
    // private List<CopybookLoader.FieldDefinition> fieldDefinitions; // Removed
    private int pageSize; // Number of records per page
    private String ebcdicEncoding;
    // private RandomAccessFile randomAccessFile; // Removed
    private transient AbstractLineReader jrecordReader; // transient if we ever serialize
    private transient LayoutDetail recordLayout;
    private List<AbstractLine> allRecords = new ArrayList<>();
    private String copybookFilePath;

    private int recordLength;
    private long totalRecords;
    private int totalPages;
    private int currentPageNumber;
    private List<Map<String, String>> currentPageRawData;

    public PagedFileReader(String filePath, String copybookFilePath, int pageSize, String ebcdicEncoding) throws IOException {
        this.filePath = filePath;
        this.copybookFilePath = copybookFilePath;
        this.pageSize = pageSize;
        this.ebcdicEncoding = ebcdicEncoding;
        this.currentPageRawData = new ArrayList<>();
        this.currentPageNumber = -1; // No page loaded initially

        // CobolIoProvider ioProvider = CobolIoProvider.getInstance(); // Not directly used for reader creation if using JRecordInterface1
        try {
            // Load copybook to get layout details (especially record length)
            ExternalRecord externalRecord = JRecordInterface1.COBOL.newIOBuilder(copybookFilePath)
                                                 .setFont(ebcdicEncoding) // Set font for copybook interpretation
                                                 .getExternalRecord(); // Changed from .load()
            this.recordLayout = externalRecord.asLayoutDetail();
            this.recordLength = this.recordLayout.getMaximumRecordLength(); // Changed from getRecordLength()

            if (this.recordLength <= 0) {
                throw new IOException("Record length is zero or invalid, cannot process file. Check copybook and encoding: " + this.recordLength);
            }

            // Calculate totalRecords based on file length and record length
            java.io.File file = new java.io.File(filePath);
            long fileLength = file.length();

            long calculatedTotalRecords; // Use a temporary variable for clarity
            if (fileLength == 0) {
                calculatedTotalRecords = 0;
            } else if (fileLength % this.recordLength != 0) {
                System.err.println("Warning: File length " + fileLength + " is not an exact multiple of record length " + this.recordLength);
                calculatedTotalRecords = fileLength / this.recordLength; // Integer division
            } else {
                calculatedTotalRecords = fileLength / this.recordLength;
            }

            if (calculatedTotalRecords > 0) {
                 // Create reader with encoding for data file
                this.jrecordReader = JRecordInterface1.COBOL.newIOBuilder(copybookFilePath)
                                                            .setFont(ebcdicEncoding) // Font for data interpretation
                                                            .newReader(filePath);
                // No need for setLayoutFont on reader if IOBuilder sets it

                // Load all records into memory
                AbstractLine line;
                while ((line = this.jrecordReader.read()) != null) {
                    allRecords.add(line);
                }
                // Update totalRecords based on actual records read, as file length calculation might be off for some complex files
                this.totalRecords = allRecords.size();
            } else {
                this.totalRecords = 0; // Ensure consistency if file was empty or only header
            }


            if (this.totalRecords == 0) {
                this.totalPages = 0;
            } else {
                this.totalPages = (int) Math.ceil((double) this.totalRecords / this.pageSize);
            }

        } catch (Exception e) { // Catch generic Exception from JRecord loading
            throw new IOException("Error initializing JRecord reader: " + e.getMessage(), e);
        } finally {
            if (jrecordReader != null) {
                try {
                    jrecordReader.close(); // Close after initial load, will reopen if needed or handle differently
                } catch (IOException e) {
                    System.err.println("Error closing JRecord reader during initialization: " + e.getMessage());
                }
            }
        }
    }

    // Removed calculateRecordLength()

    public List<Map<String, String>> getPage(int pageNumber) throws IOException {
        if (allRecords.isEmpty()) {
            this.currentPageNumber = -1;
            this.currentPageRawData = new ArrayList<>();
            return this.currentPageRawData;
        }

        if (pageNumber < 0 || pageNumber >= totalPages) {
            System.err.println("Invalid page number: " + pageNumber + ". Total pages: " + totalPages);
            // Optionally, return current page or throw exception
            if (currentPageNumber != -1 && currentPageNumber < totalPages) { // return last valid page
                 return this.currentPageRawData; // or an empty list: new ArrayList<>()
            }
            return new ArrayList<>();
        }

        List<Map<String, String>> pageData = new ArrayList<>();
        int startRecordIndex = pageNumber * pageSize;
        int endRecordIndex = Math.min(startRecordIndex + pageSize, allRecords.size());

        for (int i = startRecordIndex; i < endRecordIndex; i++) {
            AbstractLine line = allRecords.get(i);
            Map<String, String> recordMap = new HashMap<>();
            for (net.sf.JRecord.Common.FieldDetail field : recordLayout.getRecord(0).getFields()) { // Changed ExternalField to FieldDetail
                String fieldName = field.getName();
                String fieldValue = "";
                try {
                    fieldValue = line.getFieldValue(field).asString(); // Pass FieldDetail object
                } catch (Exception e) {
                    System.err.println("Error getting field value for: " + fieldName + " - " + e.getMessage());
                    fieldValue = "ERROR_READING_FIELD";
                }
                String sanitizedString = fieldValue.replaceAll("\\p{Cntrl}", ".");
                recordMap.put(fieldName, sanitizedString);
            }
            pageData.add(recordMap);
        }
        this.currentPageNumber = pageNumber;
        this.currentPageRawData = pageData;
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
        // Already on the last page or no pages, return current (which might be empty or last page's data)
        return this.currentPageRawData;
    }

    public List<Map<String, String>> previousPage() throws IOException {
        if (currentPageNumber > 0) {
            return getPage(currentPageNumber - 1);
        }
        // Already on the first page or no pages, return current (which might be empty or first page's data)
        return this.currentPageRawData;
    }

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalRecords() {
        return this.totalRecords; // Derived from allRecords.size() in constructor
    }

    public int getRecordLength() {
        return recordLength;
    }

    public net.sf.JRecord.Details.LayoutDetail getRecordLayout() {
        return this.recordLayout;
    }

    public void close() throws IOException {
        // jrecordReader is closed after initial load in the constructor.
        // If it were to be kept open for specific scenarios, closing logic would be here.
        // For now, allRecords holds the data, so no file handle is open post-constructor.
    }

    // Main method for basic testing (requires a sample EBCDIC file and copybook)
    public static void main(String[] args) {
        System.out.println("PagedFileReader.java using JRecord. To test, ensure test.cpy and test.dat exist.");
        System.out.println("Example usage:");
        System.out.println("  try {");
        System.out.println("    PagedFileReader pfr = new PagedFileReader(\"test.dat\", \"test.cpy\", 10, \"CP037\");");
        System.out.println("    System.out.println(\"Total records: \" + pfr.getTotalRecords());");
        System.out.println("    System.out.println(\"Total pages: \" + pfr.getTotalPages());");
        System.out.println("    if (pfr.getTotalPages() > 0) {");
        System.out.println("      List<Map<String, String>> page0 = pfr.getPage(0);");
        System.out.println("      // Print page0 data, e.g., page0.forEach(System.out::println);");
        System.out.println("    }");
        System.out.println("    pfr.close();");
        System.out.println("  } catch (IOException e) { e.printStackTrace(); }");

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
                 System.out.println("Creating dummy " + dataFilePath + " for basic compilation test...");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dataFilePath)) {
                    // EBCDIC for "AAAAAAAAAABBBBB" (CP037) - Approximate, actual bytes depend on exact EBCDIC variant.
                    // This is a placeholder. Real EBCDIC data needed for full test.
                    byte[] rec1 = {(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1,(byte)0xC1, (byte)0xC2,(byte)0xC2,(byte)0xC2,(byte)0xC2,(byte)0xC2}; // AAAAABBBBB
                    byte[] rec2 = {(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3,(byte)0xC3, (byte)0xC4,(byte)0xC4,(byte)0xC4,(byte)0xC4,(byte)0xC4}; // CCCCCDDDDD
                    if (rec1.length == 15 && rec2.length == 15) { // Assuming record length 15 from dummy cpy
                         fos.write(rec1);
                         fos.write(rec2);
                    } else {
                        System.err.println("Dummy EBCDIC byte array length mismatch, check dummy data generation.");
                    }
                }
            }
            System.out.println("\n--- Running Test with Dummy Files (JRecord) ---");

            // Informational: Load with JRecord to show expected record length
            try {
                ExternalRecord extRecInfo = JRecordInterface1.COBOL.newIOBuilder(copybookFilePath).getExternalRecord();
                System.out.println("Record length from JRecord for info: " + extRecInfo.asLayoutDetail().getMaximumRecordLength());
            } catch (Exception e) {
                System.out.println("Error using JRecord to get copybook info for main method: " + e.getMessage());
            }

            PagedFileReader pfr = new PagedFileReader(dataFilePath, copybookFilePath, 1, "CP037"); // Page size 1
            System.out.println("Total records (JRecord): " + pfr.getTotalRecords());
            System.out.println("Total pages (JRecord): " + pfr.getTotalPages());
            System.out.println("Record Length (JRecord): " + pfr.getRecordLength());


            if (pfr.getTotalPages() > 0) {
                List<Map<String, String>> page0 = pfr.getPage(0);
                System.out.println("\nPage 0 Data (JRecord - first record if exists):");
                if (!page0.isEmpty()) {
                    System.out.println(page0.get(0));
                } else {
                    System.out.println("Page 0 is empty.");
                }
                 if (pfr.getTotalPages() > 1) {
                    List<Map<String, String>> page1 = pfr.nextPage();
                     System.out.println("\nPage 1 Data (JRecord - first record if exists):");
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
            System.out.println("\n--- Test with Dummy Files (JRecord) Finished ---");

        } catch (IOException e) {
            System.err.println("Error during JRecord dummy file test setup or PagedFileReader execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
