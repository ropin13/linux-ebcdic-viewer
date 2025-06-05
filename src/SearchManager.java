import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchManager {

    /**
     * Searches a list of records (page data) for a given term in a specified field.
     * The search is case-insensitive.
     *
     * @param pageData The list of records (maps) to search.
     * @param fieldName The name of the field to search within.
     * @param searchTerm The term to search for.
     * @return A new list of records that match the search criteria.
     *         Returns an empty list if pageData is null, fieldName is null/empty,
     *         or no matches are found.
     */
    public List<Map<String, String>> search(List<Map<String, String>> pageData, String fieldName, String searchTerm) {
        List<Map<String, String>> searchResults = new ArrayList<>();

        if (pageData == null || fieldName == null || fieldName.trim().isEmpty() || searchTerm == null) {
            // It might be better to throw IllegalArgumentException for null/empty fieldName
            // or null pageData, but for now, returning empty list for simplicity.
            return searchResults;
        }

        String lowerCaseSearchTerm = searchTerm.toLowerCase();

        for (Map<String, String> record : pageData) {
            String fieldValue = record.get(fieldName);
            if (fieldValue != null) {
                if (fieldValue.toLowerCase().contains(lowerCaseSearchTerm)) {
                    searchResults.add(record); // Add the original record
                }
            }
        }
        return searchResults;
    }

    /**
     * Main method for basic testing of the SearchManager.
     */
    public static void main(String[] args) {
        SearchManager searchManager = new SearchManager();

        // Sample data
        List<Map<String, String>> samplePageData = new ArrayList<>();
        Map<String, String> record1 = new HashMap<>();
        record1.put("ID", "101");
        record1.put("NAME", "John Doe");
        record1.put("CITY", "New York");
        samplePageData.add(record1);

        Map<String, String> record2 = new HashMap<>();
        record2.put("ID", "102");
        record2.put("NAME", "Jane Smith");
        record2.put("CITY", "London");
        samplePageData.add(record2);

        Map<String, String> record3 = new HashMap<>();
        record3.put("ID", "103");
        record3.put("NAME", "Peter Jones");
        record3.put("CITY", "New York");
        samplePageData.add(record3);

        Map<String, String> record4 = new HashMap<>();
        record4.put("ID", "104");
        record4.put("NAME", "Alice Brown");
        record4.put("CITY", "Paris");
        record4.put("NOTES", "Met John in New York");
        samplePageData.add(record4);


        System.out.println("Original Data:");
        for (Map<String, String> record : samplePageData) {
            System.out.println(record);
        }

        // Test case 1: Search by NAME
        String searchField1 = "NAME";
        String searchTerm1 = "john";
        System.out.println("\nSearching for '" + searchTerm1 + "' in field '" + searchField1 + "':");
        List<Map<String, String>> results1 = searchManager.search(samplePageData, searchField1, searchTerm1);
        if (results1.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (Map<String, String> record : results1) {
                System.out.println(record);
            }
        }

        // Test case 2: Search by CITY
        String searchField2 = "CITY";
        String searchTerm2 = "New York";
        System.out.println("\nSearching for '" + searchTerm2 + "' in field '" + searchField2 + "':");
        List<Map<String, String>> results2 = searchManager.search(samplePageData, searchField2, searchTerm2);
        if (results2.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (Map<String, String> record : results2) {
                System.out.println(record);
            }
        }

        // Test case 3: Search term not found
        String searchField3 = "NAME";
        String searchTerm3 = "David";
        System.out.println("\nSearching for '" + searchTerm3 + "' in field '" + searchField3 + "':");
        List<Map<String, String>> results3 = searchManager.search(samplePageData, searchField3, searchTerm3);
        if (results3.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (Map<String, String> record : results3) {
                System.out.println(record);
            }
        }

        // Test case 4: Search in a field that doesn't exist in all records
        String searchField4 = "NOTES";
        String searchTerm4 = "new york";
        System.out.println("\nSearching for '" + searchTerm4 + "' in field '" + searchField4 + "':");
        List<Map<String, String>> results4 = searchManager.search(samplePageData, searchField4, searchTerm4);
        if (results4.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (Map<String, String> record : results4) {
                System.out.println(record);
            }
        }

        // Test case 5: Null page data
        System.out.println("\nSearching with null page data:");
        List<Map<String, String>> results5 = searchManager.search(null, "NAME", "john");
         if (results5.isEmpty()) {
            System.out.println("No results found (as expected for null input).");
        } else {
            for (Map<String, String> record : results5) {
                System.out.println(record);
            }
        }
    }
}
