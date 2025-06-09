import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// JRecord classes for LayoutDetail and FieldDetail
import net.sf.JRecord.Details.LayoutDetail;
import net.sf.JRecord.Common.FieldDetail;

public class AppController {
    // private CopybookLoader copybookLoader; // Removed
    private PagedFileReader pagedFileReader;
    private SearchManager searchManager;
    private TUIView tuiView;

    private List<Map<String, String>> currentPageData; // Original data for the current page (raw from file)
    private List<Map<String, String>> displayedData;   // Data currently shown (could be original or search results)
    private boolean isSearchActive = false;
    private String currentStatusMessage = "";
    private String dataFilePath;
    private String copybookFilePath;
    private String encoding;
    private int pageSize;


    public AppController(String dataFilePath, String copybookFilePath, String encoding, int pageSize) {
        this.dataFilePath = dataFilePath;
        this.copybookFilePath = copybookFilePath;
        this.encoding = encoding;
        this.pageSize = pageSize;

        // TUIView must be initialized first to display any early errors.
        this.tuiView = new TUIView(this);

        try {
            // Initialize PagedFileReader first
            this.pagedFileReader = new PagedFileReader(dataFilePath, copybookFilePath, pageSize, encoding);
            this.searchManager = new SearchManager();

            // Get layout and field names from PagedFileReader
            LayoutDetail layout = pagedFileReader.getRecordLayout();
            if (layout == null || layout.getRecordCount() == 0 || layout.getRecord(0).getFieldCount() == 0) {
                // Handle error: No fields defined by JRecord from the copybook
                String errorMessage = "Fatal Error: JRecord could not find field definitions in copybook: " + copybookFilePath + ". Application will exit.";
                System.err.println(errorMessage);
                if (this.tuiView != null) {
                    this.tuiView.init(new ArrayList<>()); // Pass empty list for field names
                    this.tuiView.displayError(errorMessage);
                    try { Thread.sleep(4000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    this.tuiView.close();
                }
                System.exit(1);
                return; // Important to prevent further execution
            }
            // Extract field names for TUIView
            List<String> fieldNames = new ArrayList<>();
            for (FieldDetail fieldDetail : layout.getRecord(0).getFields()) {
                fieldNames.add(fieldDetail.getName());
            }
            // Pass the List<String> of field names to TUIView's init method.
            this.tuiView.init(fieldNames);

        } catch (IOException e) {
            System.err.println("Error initializing application components: " + e.getMessage());
            e.printStackTrace();
            if (this.tuiView != null) {
                this.tuiView.init(new ArrayList<>());
                this.tuiView.displayError("Fatal Initialization Error: " + e.getMessage() + ". Application will exit.");
                try { Thread.sleep(4000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                this.tuiView.close();
            }
            System.exit(1);
        } catch (Exception e) {
             System.err.println("Unexpected error during application initialization: " + e.getMessage());
            e.printStackTrace();
            if (this.tuiView != null) {
                this.tuiView.init(new ArrayList<>());
                this.tuiView.displayError("Fatal Unexpected Initialization Error: " + e.getMessage() + ". Application will exit.");
                try { Thread.sleep(4000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                this.tuiView.close();
            }
            System.exit(1);
        }
    }

    public void start() {
        if (pagedFileReader.getTotalRecords() == 0) {
            if (pagedFileReader.getRecordLength() > 0) {
                 currentStatusMessage = "Data file appears empty or is too small for defined records.";
            } else {
                 currentStatusMessage = "No records to display. Check copybook and data file.";
            }
             this.currentPageData = new ArrayList<>();
             this.displayedData = new ArrayList<>();
        } else {
             // Load first page (0-indexed), TUIView will display 1-based.
             loadPageAndDisplay(0, "Page 1 of " + pagedFileReader.getTotalPages() + ".");
        }
        // Initial display, potentially with empty data message
        // This call is essential to show the UI before handleInput blocks
        updateTUIView();

        if (tuiView != null) {
            tuiView.handleInput(); // Start listening for user input
            tuiView.close();       // Ensure TUI is closed when handleInput exits
        }
    }

    private void loadPageAndDisplay(int pageNumber, String message) {
        try {
            if (pagedFileReader.getTotalPages() == 0 && pageNumber == 0) {
                 this.currentPageData = new ArrayList<>();
                 this.currentStatusMessage = message != null ? message : "No data in file.";
            } else if (pageNumber < 0 || pageNumber >= pagedFileReader.getTotalPages()) {
                this.currentStatusMessage = "Invalid page number requested: " + (pageNumber + 1);
                updateTUIView(); // Update status bar with error
                return;
            } else {
                this.currentPageData = pagedFileReader.getPage(pageNumber);
                this.currentStatusMessage = message != null ? message : "Page " + (pagedFileReader.getCurrentPageNumber() + 1) + " of " + pagedFileReader.getTotalPages() + ".";
            }
            this.displayedData = this.currentPageData;
            this.isSearchActive = false;
            updateTUIView();
        } catch (IOException e) {
            e.printStackTrace();
            this.currentStatusMessage = "Error loading page " + (pageNumber + 1) + ": " + e.getMessage();
            this.displayedData = new ArrayList<>();
            updateTUIView();
        }
    }

    public void requestNextPage() {
        if (isSearchActive) {
            currentStatusMessage = "On search results. Clear search ('c') to navigate pages.";
            updateTUIView();
            return;
        }
        if (pagedFileReader.getCurrentPageNumber() < pagedFileReader.getTotalPages() - 1) {
            loadPageAndDisplay(pagedFileReader.getCurrentPageNumber() + 1, null);
        } else {
            currentStatusMessage = "Already on the last page ("+ pagedFileReader.getTotalPages() +").";
            updateTUIView();
        }
    }

    public void requestPreviousPage() {
        if (isSearchActive) {
            currentStatusMessage = "On search results. Clear search ('c') to navigate pages.";
            updateTUIView();
            return;
        }
        if (pagedFileReader.getCurrentPageNumber() > 0) {
            loadPageAndDisplay(pagedFileReader.getCurrentPageNumber() - 1, null);
        } else {
            currentStatusMessage = "Already on the first page (1).";
            updateTUIView();
        }
    }

    public void performSearch(String fieldName, String searchTerm) {
        List<Map<String, String>> dataToSearch = pagedFileReader.getCurrentPageRawData();
        if (dataToSearch == null || dataToSearch.isEmpty()) {
            currentStatusMessage = "No data on the current page (" + (pagedFileReader.getCurrentPageNumber() + 1) + ") to search.";
            this.displayedData = new ArrayList<>();
            updateTUIView();
            return;
        }

        this.displayedData = searchManager.search(dataToSearch, fieldName, searchTerm);
        this.isSearchActive = true;
        this.currentStatusMessage = String.format("Search for '%s' in '%s' on page %d: %d results.",
            searchTerm, fieldName, pagedFileReader.getCurrentPageNumber() + 1, displayedData.size());
        updateTUIView();
    }

    public void clearSearch() {
        if (!isSearchActive && (displayedData == currentPageData)) {
            currentStatusMessage = "No active search to clear. Displaying page " + (pagedFileReader.getCurrentPageNumber() + 1) + ".";
        } else {
            this.displayedData = this.currentPageData;
            this.isSearchActive = false;
            this.currentStatusMessage = "Search cleared. Displaying page " + (pagedFileReader.getCurrentPageNumber() + 1) + ".";
        }
        updateTUIView();
    }

    private void updateTUIView() {
        if (tuiView == null) return;

        List<Map<String, String>> dataForDisplay = (displayedData != null) ? displayedData : new ArrayList<>();

        // currentPage in TUIView is 0-indexed for data, 1-indexed for display
        int currentPageForDisplay = pagedFileReader.getTotalPages() == 0 ? 0 : pagedFileReader.getCurrentPageNumber();

        tuiView.displayData(
            dataForDisplay,
            currentPageForDisplay,
            pagedFileReader.getTotalPages(),
            pagedFileReader.getTotalRecords(),
            this.encoding,
            currentStatusMessage
        );
    }

    public void quit() {
        // This method is called by TUIView when 'q' is pressed.
        // The main loop in TUIView's handleInput() will terminate,
        // and then tuiView.close() will be called in AppController's start() method.
        // No explicit action needed here other than to exist for TUIView to call.
        System.out.println("Quit requested from TUI.");
    }


    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java AppController <data_file_path> <copybook_file_path> <ebcdic_encoding> <page_size>");
            System.out.println("Example: java -cp \"lib/lanterna-3.1.1.jar:src\" AppController data/INPUT.DAT data/COBCOPYA.CPY CP037 100");

            if (args.length == 0) {
                 System.out.println("\nNo arguments provided. Attempting to run with dummy files (data/test.dat, data/test.cpy)...");
                try {
                    new java.io.File("data").mkdirs();
                    String copybookFilePath = "data/test.cpy";
                    String dataFilePath = "data/test.dat";

                    java.io.File cpyFile = new java.io.File(copybookFilePath);
                    if (!cpyFile.exists()) {
                        System.out.println("Creating dummy " + copybookFilePath);
                        try (java.io.PrintWriter writer = new java.io.PrintWriter(copybookFilePath)) {
                            writer.println("01 RECORD-LAYOUT.");
                            writer.println("  05 NAME PIC X(10).");
                            writer.println("  05 VALUE PIC X(5).");
                            writer.println("  05 INFO PIC X(15)."); // Total 30 bytes
                        }
                    }

                    java.io.File datFile = new java.io.File(dataFilePath);
                    // Ensure file is overwritten by not checking for existence or length before writing
                    System.out.println("Creating/Overwriting dummy " + dataFilePath + " with EBCDIC (CP037) data. Each record 30 bytes.");
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dataFilePath, false)) { // false for overwrite
                        // Ensure strings are padded to exact field lengths before converting to EBCDIC
                        String rec1_name = String.format("%-10s", "TEST001");
                            String rec1_val = String.format("%-5s", "V01");
                            String rec1_info = String.format("%-15s", "EBCDIC DATA 001");
                            fos.write((rec1_name + rec1_val + rec1_info).getBytes("CP037"));

                            String rec2_name = String.format("%-10s", "TEST002");
                            String rec2_val = String.format("%-5s", "V02");
                            String rec2_info = String.format("%-15s", "EBCDIC DATA 002");
                            fos.write((rec2_name + rec2_val + rec2_info).getBytes("CP037"));

                            String rec3_name = String.format("%-10s", "ANOTHER");
                            String rec3_val = String.format("%-5s", "V03");
                            String rec3_info = String.format("%-15s", "EBCDIC DATA 003");
                            fos.write((rec3_name + rec3_val + rec3_info).getBytes("CP037"));

                            String rec4_name = String.format("%-10s", "CHECKTHIS");
                            String rec4_val = String.format("%-5s", "V04");
                            String rec4_info = String.format("%-15s", "EBCDIC DATA 004");
                            fos.write((rec4_name + rec4_val + rec4_info).getBytes("CP037"));
                        }
                    // } // This '}' was prematurely closing the main 'try' block for dummy data.
                    // The AppController instantiation and start must be inside the main try.
                    AppController controller = new AppController(dataFilePath, copybookFilePath, "CP037", 2); // page size 2
                    controller.start();

                } catch (Exception e) {
                    System.err.println("Error during dummy file test setup or AppController execution: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return;
        }

        String dataFile = args[0];
        String copybookFile = args[1];
        String encoding = args[2];
        int pageSize;
        try {
            pageSize = Integer.parseInt(args[3]);
            if (pageSize <=0) {
                 System.err.println("Page size must be a positive integer.");
                 return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid page size: " + args[3] + ". Must be an integer.");
            return;
        }

        AppController controller = new AppController(dataFile, copybookFile, encoding, pageSize);
        controller.start();
    }
}
