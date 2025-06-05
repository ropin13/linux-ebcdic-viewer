import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopybookLoader {

    private List<FieldDefinition> fieldDefinitions;

    public static class FieldDefinition {
        private String name;
        private int startPosition; // Assuming calculation based on previous fields
        private int length;
        private String type; // PIC X for now

        public FieldDefinition(String name, int length, String type) {
            this.name = name;
            this.length = length;
            this.type = type;
            // startPosition will be calculated later
        }

        public String getName() {
            return name;
        }

        public int getStartPosition() {
            return startPosition;
        }

        public void setStartPosition(int startPosition) {
            this.startPosition = startPosition;
        }

        public int getLength() {
            return length;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "FieldDefinition{" +
                    "name='" + name + '\'' +
                    ", startPosition=" + startPosition +
                    ", length=" + length +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    public CopybookLoader(String filePath) {
        this.fieldDefinitions = new ArrayList<>();
        parseCopybook(filePath);
        calculateStartPositions();
    }

    private void parseCopybook(String filePath) {
        // Regex to capture: level (optional), field name, PIC X(length)
        // Example: 05 FIELD-NAME PIC X(10).
        // Pattern allows for optional level number, captures field name and length for PIC X.
        Pattern pattern = Pattern.compile("^\\s*\\d*\\s+([A-Z0-9-]+)\\s+PIC\\s+X\\((\\d+)\\)\\s*\\.$", Pattern.CASE_INSENSITIVE);


        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line.trim());
                if (matcher.matches()) {
                    String fieldName = matcher.group(1);
                    int length = Integer.parseInt(matcher.group(2));
                    // Assuming type PIC X for now
                    fieldDefinitions.add(new FieldDefinition(fieldName, length, "PIC X"));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading copybook file: " + filePath + " - " + e.getMessage());
            // Depending on requirements, might throw a custom exception or handle differently
        } catch (NumberFormatException e) {
            System.err.println("Error parsing length in copybook file: " + filePath + " - " + e.getMessage());
        }
    }

    private void calculateStartPositions() {
        int currentPosition = 1; // Start positions are typically 1-based
        for (FieldDefinition field : fieldDefinitions) {
            field.setStartPosition(currentPosition);
            currentPosition += field.getLength();
        }
    }

    public List<FieldDefinition> getFieldDefinitions() {
        return fieldDefinitions;
    }

    // Optional: Main method for testing
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java CopybookLoader <path_to_copybook_file>");
            return;
        }
        CopybookLoader loader = new CopybookLoader(args[0]);
        if (loader.getFieldDefinitions().isEmpty() && !new java.io.File(args[0]).exists()) {
             // Error message already printed by parseCopybook for non-existent file
            return;
        }
        if (loader.getFieldDefinitions().isEmpty()) {
            System.out.println("No field definitions found or parsed from " + args[0]);
        } else {
            System.out.println("Parsed Field Definitions from " + args[0] + ":");
            for (FieldDefinition field : loader.getFieldDefinitions()) {
                System.out.println(field);
            }
        }
    }
}
