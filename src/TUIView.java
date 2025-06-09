import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
// import com.googlecode.lanterna.gui2.SGR; // Explicitly using com.googlecode.lanterna.SGR
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
// import com.googlecode.lanterna.gui2.dialogs.ComboBoxDialogBuilder; // Temporarily replacing with TextInputDialogBuilder
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TUIView {
    private Terminal terminal;
    private Screen screen;
    private WindowBasedTextGUI textGUI;
    private AppController appController;
    private List<String> fieldNames; // Changed from List<CopybookLoader.FieldDefinition>
    private BasicWindow window;
    private Panel mainPanel;
    private Label statusLabel;
    private Label commandLabel;
    private Panel tablePanel; // Panel to hold the table-like structure

    private static final int MIN_TERMINAL_WIDTH = 80;
    private static final int MIN_TERMINAL_HEIGHT = 24;


    public TUIView(AppController appController) {
        this.appController = appController;
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            this.terminal = factory.createTerminal();
            this.screen = new TerminalScreen(this.terminal);
            this.textGUI = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
            this.textGUI.setTheme(LanternaThemes.getRegisteredTheme("default")); // or another theme
            screen.startScreen();

            // Check terminal size
            TerminalSize terminalSize = screen.getTerminalSize();
            if (terminalSize.getColumns() < MIN_TERMINAL_WIDTH || terminalSize.getRows() < MIN_TERMINAL_HEIGHT) {
                screen.stopScreen();
                System.err.println("Terminal window is too small. Please resize to at least " +
                                   MIN_TERMINAL_WIDTH + "x" + MIN_TERMINAL_HEIGHT + " and restart.");
                // appController.quit() might be needed if appController is fully initialized
                System.exit(1); // Exit if terminal is too small
            }


            this.window = new BasicWindow("EBCDIC Data Viewer");
            this.window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
            this.mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));

            statusLabel = new Label("Status: Initializing...");
            commandLabel = new Label("Commands: (n)Next (p)Prev (s)Search (c)ClearSearch (q)Quit");
            tablePanel = new Panel(); // Layout will be set in displayData

            mainPanel.addComponent(statusLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Beginning, null))); // Try null for GrowPolicy
            mainPanel.addComponent(tablePanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, null))); // Try null for GrowPolicy
            mainPanel.addComponent(commandLabel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.End, null))); // Try null for GrowPolicy

            window.setComponent(mainPanel);

        } catch (IOException e) {
            e.printStackTrace();
            // Ensure cleanup if initialization fails partially
            if (screen != null) { // Removed isStarted()
                try {
                    // Check if screen is actually started before stopping, though stopScreen() itself should handle it
                    if (screen.getTerminalSize() != null && screen.getTerminalSize().getColumns() > 0) { // A proxy for started
                         screen.stopScreen();
                    }
                } catch (IOException stopEx) {
                    stopEx.printStackTrace();
                }
            }
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException closeEx) {
                    closeEx.printStackTrace();
                }
            }
            // Propagate or handle critical failure
            throw new RuntimeException("Failed to initialize TUI", e);
        }
    }

    public void init(List<String> fieldNames) { // Signature changed
        this.fieldNames = fieldNames; // Changed from fieldDefs
        if (fieldNames == null || fieldNames.isEmpty()) { // Changed from fieldDefs
            // Display a message if no field definitions are available
            // This could happen if copybook parsing failed or copybook was empty
            displayError("No field definitions loaded. Cannot display data.");
        }
    }

    public void displayError(String errorMessage) {
        MessageDialog.showMessageDialog(textGUI, "Error", errorMessage);
    }

    public void displayData(List<Map<String, String>> pageData, int currentPage, int totalPages, long totalRecords, String encoding, String currentStatusMessage) {
        if (this.fieldNames == null || this.fieldNames.isEmpty()) { // Changed from fieldDefinitions
            statusLabel.setText("Status: No field definitions. Cannot display page.");
            tablePanel.removeAllComponents(); // Clear any previous table
            // Optionally, add a label to tablePanel indicating the issue
            Label errorLabel = new Label("Error: Field definitions are missing. Please check copybook loading.");
            tablePanel.addComponent(errorLabel);
            try {
                textGUI.updateScreen(); // Refresh to show the error message
            } catch (IOException e) {
                System.err.println("IOException during textGUI.updateScreen() in displayData (empty field definitions): " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        statusLabel.setText(String.format("Page: %d/%d | Records: %d | Encoding: %s | %s",
                (totalPages == 0 && totalRecords == 0) ? 0 : currentPage + 1, // Display 1-based page number
                totalPages, totalRecords, encoding, currentStatusMessage));

        tablePanel.removeAllComponents(); // Clear previous table content

        if (pageData == null || pageData.isEmpty()) {
            tablePanel.addComponent(new Label("No data to display for this page."));
        } else {
            // Determine column widths dynamically or use fixed widths
            // For simplicity, let's try to give them some space
            // This is a very basic way to do it, a more robust Table component from Lanterna extensions or a custom one would be better.

            // Create a basic grid-like layout for the table
            Panel gridPanel = new Panel(new GridLayout(this.fieldNames.size())); // Changed from fieldDefinitions
            gridPanel.addComponent(new EmptySpace(new TerminalSize(0,0))); // Top-left empty cell if needed for borders

            // Add headers
            for (String fieldName : this.fieldNames) { // Changed from fieldDefinitions
                gridPanel.addComponent(new Label(fieldName).addStyle(com.googlecode.lanterna.SGR.BOLD));
            }

            // Add data rows
            for (Map<String, String> record : pageData) {
                for (String fieldName : this.fieldNames) { // Changed from fieldDefinitions
                    String value = record.get(fieldName); // Use fieldName directly
                    gridPanel.addComponent(new Label(value != null ? value : ""));
                }
            }
            tablePanel.addComponent(gridPanel.withBorder(Borders.singleLine("Data")));
        }

        // Ensure the window is active and refresh
        // textGUI.addWindowAndWait(window); // This blocks, use addWindow and then handle input loop
        if (textGUI.getWindows().isEmpty()) { // Only add if not already added
             textGUI.addWindow(window);
        }
        try {
            textGUI.updateScreen(); // More fine-grained update
        } catch (IOException e) { // Catch specific IOException
            System.err.println("IOException during textGUI.updateScreen(): " + e.getMessage());
            // Lanterna can sometimes throw exceptions during update if terminal is resized badly
            e.printStackTrace();
        }
    }


    public void handleInput() {
        if (screen == null){ // Removed isStarted() check
            System.err.println("Screen not initialized, cannot handle input.");
            return;
        }
        final AtomicBoolean running = new AtomicBoolean(true);

        // This is a simplified input loop. For GUI applications, events are usually handled by the GUI toolkit.
        // However, for some direct keybindings not tied to specific components, this can be useful.
        // For a full Lanterna GUI2 app, you'd typically add key listeners to windows/panels or use Actions.

        // window.setFocusedInteractable(mainPanel); // Removed: Panel is not Interactable. Focus managed by GUI.

        // Add global key listeners to the window
        window.addWindowListener(new WindowListenerAdapter() {
            @Override // Assuming this signature is correct now with Window
            public void onInput(Window w, KeyStroke k, AtomicBoolean deliverEvent) {
                 if (k.getKeyType() == KeyType.Character) {
                    switch (k.getCharacter()) {
                        case 'n':
                            appController.requestNextPage();
                            break;
                        case 'p':
                            appController.requestPreviousPage();
                            break;
                        case 's':
                            promptForSearch();
                            break;
                        case 'c':
                            appController.clearSearch();
                            break;
                        case 'q':
                            running.set(false); // Signal to exit the loop
                            appController.quit();
                            break;
                    }
                } else if (k.getKeyType() == KeyType.Escape) {
                     // Could also trigger quit or a menu
                }
                 // Need to refresh the display based on action
                 // This should be ideally triggered by AppController after state changes
                 // For now, let's assume AppController's methods will call back to TUIView to refresh
                 deliverEvent.set(true); // Allow other components to process the event too if needed
            }
        });


        // The main loop for a Lanterna GUI application is typically textGUI.waitForWindowToClose(window);
        // The input handling is done via listeners attached to components or the window itself.
        // The loop below is more for non-GUI or mixed mode, but since we use GUI components,
        // the listeners are the primary way.
        // The `running` flag controlled by 'q' will eventually lead to closing the window.

        while(running.get() && textGUI.getActiveWindow() != null) {
            // Process GUI events. This is essential.
            // If there are no windows, textGUI.processInput() will return false.
            if (!textGUI.getWindows().isEmpty()) {
                try {
                    textGUI.processInput();
                } catch (IOException e) {
                    System.err.println("IOException during textGUI.processInput(): " + e.getMessage());
                    e.printStackTrace();
                    running.set(false); // Exit loop on input processing error
                }
            } else {
                running.set(false); // No windows left, so exit
            }
            try {
                Thread.sleep(10); // Small delay to prevent busy-waiting if processInput is non-blocking
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }

        // After loop finishes (e.g. 'q' pressed, or window closed)
        close();
    }


    private void promptForSearch() {
        if (this.fieldNames == null || this.fieldNames.isEmpty()) { // Changed from fieldDefinitions
            MessageDialog.showMessageDialog(textGUI, "Search Error", "No fields available to search.");
            return;
        }

        // Create a list of field names for the user to choose from
        List<String> fieldNamesForPrompt = new ArrayList<>(this.fieldNames); // Use this.fieldNames

        String fieldListForPrompt = "Available fields: " + String.join(", ", fieldNamesForPrompt);
        if (fieldNamesForPrompt.isEmpty()) { // Check the local list
            fieldListForPrompt = "No searchable fields defined.";
        }

        // Prompt for field name using TextInputDialogBuilder as a workaround
        String fieldToSearch = new TextInputDialogBuilder()
                .setTitle("Search Field")
                .setDescription(fieldListForPrompt)
                .setTextBoxSize(new TerminalSize(30,1))
                .setInitialContent(fieldNamesForPrompt.isEmpty() ? "" : fieldNamesForPrompt.get(0)) // Use local list
                .setValidator((text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        return "Field name cannot be empty.";
                    }
                    if (fieldNamesForPrompt.isEmpty() || !fieldNamesForPrompt.contains(text.trim())) { // Use local list
                        return "Invalid field. Please choose from list above.";
                    }
                    return null;
                })
                .build()
                .showDialog(textGUI);

        if (fieldToSearch == null) { // User cancelled or input was invalid (and validator returned non-null, though showDialog would typically re-prompt)
            return;
        }

        // Prompt for search term
        String searchTerm = new TextInputDialogBuilder()
                .setTitle("Search Term")
                .setDescription("Enter text to search for in '" + fieldToSearch + "':")
                .setTextBoxSize(new TerminalSize(30, 1))
                .setInitialContent("")
                .setValidator((content) -> null) // No validation for now
                .build()
                .showDialog(textGUI);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            appController.performSearch(fieldToSearch, searchTerm);
        } else if (searchTerm != null && searchTerm.isEmpty()){
             MessageDialog.showMessageDialog(textGUI, "Search Info", "Search term was empty. To clear search results, use 'c'.");
        }
    }

    public void close() {
        try {
            if (screen != null) { // Removed isStarted()
                // Check if screen is actually started before stopping
                 if (screen.getTerminalSize() != null && screen.getTerminalSize().getColumns() > 0) { // A proxy for started
                    screen.stopScreen();
                }
            }
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TUI closed.");
    }

    // Main method for isolated testing has been removed as AppController.main() is the primary entry point.
}
