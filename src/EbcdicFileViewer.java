public class EbcdicFileViewer {

    private static final String DEFAULT_ENCODING = "IBM037"; // Common EBCDIC encoding
    private static final int DEFAULT_PAGE_SIZE = 50;     // Default number of records per page

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            printUsage();
            System.exit(1);
        }

        String dataFilePath = args[0];
        String copybookFilePath = args[1];
        String encoding = DEFAULT_ENCODING;
        int pageSize = DEFAULT_PAGE_SIZE;

        if (args.length >= 3) {
            // Validate encoding string? For now, assume it's valid.
            // A more robust solution might check against Charset.isSupported(args[2])
            // but that can be complex depending on available charsets in the JRE.
            encoding = args[2];
        }

        if (args.length == 4) {
            try {
                pageSize = Integer.parseInt(args[3]);
                if (pageSize <= 0) {
                    System.err.println("Error: Page size must be a positive integer.");
                    printUsage();
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid page size '" + args[3] + "'. Please provide an integer.");
                printUsage();
                System.exit(1);
            }
        }

        // Check if files exist (basic check)
        // Note: AppController's PagedFileReader will do more robust file handling,
        // but an early check here can provide a more immediate user-friendly message.
        java.io.File dataFile = new java.io.File(dataFilePath);
        if (!dataFile.exists() || !dataFile.isFile()) {
            System.err.println("Error: Data file not found or is not a regular file: " + dataFilePath);
            printUsage();
            System.exit(1);
        }

        java.io.File copybookFile = new java.io.File(copybookFilePath);
        if (!copybookFile.exists() || !copybookFile.isFile()) {
            System.err.println("Error: Copybook file not found or is not a regular file: " + copybookFilePath);
            printUsage();
            System.exit(1);
        }


        // Assuming AppController is in the same package (default) or properly imported.
        try {
            AppController appController = new AppController(dataFilePath, copybookFilePath, encoding, pageSize);
            appController.start();
        } catch (Exception e) {
            // Catch any unexpected exceptions during AppController init or start that might not have been handled internally by AppController's System.exit
            System.err.println("An unexpected critical error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1); // Exit with a generic error code for unexpected issues
        }
    }

    private static void printUsage() {
        System.err.println("\nEbcdic File Viewer");
        System.err.println("--------------------");
        System.err.println("Displays EBCDIC encoded data files based on a COBOL copybook definition.");
        System.err.println("\nUsage: java EbcdicFileViewer <data_file_path> <copybook_file_path> [encoding] [page_size]");
        System.err.println("\nArguments:");
        System.err.println("  <data_file_path>    : Path to the EBCDIC data file (required).");
        System.err.println("  <copybook_file_path>: Path to the COBOL copybook file (required).");
        System.err.println("  [encoding]          : EBCDIC encoding to use (optional, defaults to " + DEFAULT_ENCODING + ").");
        System.err.println("                      Common examples: IBM037, CP037, IBM500, IBM1047.");
        System.err.println("  [page_size]         : Number of records per page (optional, defaults to " + DEFAULT_PAGE_SIZE + ").");
        System.err.println("\nExample:");
        System.err.println("  java -cp \"lib/lanterna-3.1.1.jar:src\" EbcdicFileViewer data/EBCDIC.DAT layout/COBCOPY.CPY IBM037 75");
        System.err.println("  (If using packages, replace EbcdicFileViewer with fully qualified class name, e.g. com.example.EbcdicFileViewer)");
    }
}
