package gr.uop.scenes;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MenuFactory {
    private static MenuBar menuBar = new MenuBar();
    private static Menu gameMenu = new Menu("Παιχνίδι");
    private static MenuItem playGameItem = new MenuItem("Συμμετοχή σε παιχνίδι");
    private static MenuItem stopGameItem = new MenuItem("Διακοπή παιχνιδιού");
    private static MenuItem exitItem = new MenuItem("Έξοδος");
    private static Menu helpMenu = new Menu("Βοήθεια");
    private static MenuItem helpItem = new MenuItem("Βοήθεια");
    private static MenuItem aboutItem = new MenuItem("Σχετικά με τον Γκρινιάρη");

    public static MenuBar createMenuBar() {
        menuBar = new MenuBar();
        gameMenu = new Menu("Παιχνίδι");
        playGameItem = new MenuItem("Συμμετοχή σε παιχνίδι");
        stopGameItem = new MenuItem("Διακοπή παιχνιδιού");
        exitItem = new MenuItem("Έξοδος");
        exitItem.setOnAction(e -> {
            // Ερωτηση επιβεβαίωσης πριν την έξοδο
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Έξοδος");
            alert.setHeaderText(null);
            alert.setContentText("Έξοδος από την εφαρμογή.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.exit(0);
                }
            });
        });
        Menu helpMenu = new Menu("Βοήθεια");
        MenuItem helpItem = new MenuItem("Βοήθεια");
        MenuItem aboutItem = new MenuItem("Σχετικά με τον Γκρινιάρη");

        // New: show a modal window with information about Ludo
        helpItem.setOnAction(e -> {
            Stage helpStage = new Stage();
            helpStage.setTitle("Βοήθεια");

            Label title = new Label("Σύντομη περιγραφή για το παιχνίδι.");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            TextArea content = new TextArea(
                "• Το παιχνίδι παίζεται από 2 έως 4 παίκτες.\n\n" +
                "• Κάθε παίκτης έχει 4 πιόνια ίδιου χρώματος.\n\n" +
                "• Ο στόχος είναι να φέρεις όλα τα πιόνια σου στη βάση τερματισμού πριν από τους άλλους.\n\n" +
                "• Για να ξεκινήσει ένα πιόνι από τη βάση, πρέπει να φέρεις 6 στο ζάρι.\n\n" +
                "• Όταν φέρεις 6, μπορείς είτε να ξεκινήσεις νέο πιόνι είτε να ξαναρίξεις το ζάρι.\n\n" +
                "• Τα πιόνια κινούνται με τη φορά του ρολογιού γύρω από το ταμπλό.\n\n" +
                "• Αν ένα πιόνι σταματήσει σε θέση που ήδη υπάρχει πιόνι άλλου παίκτη, το πιόνι αυτό επιστρέφει στη βάση.\n\n" +
                "• Τα πιόνια του ίδιου χρώματος δεν μπορούν να “φαγωθούν” μεταξύ τους.\n\n" +
                "• Κάθε παίκτης ρίχνει το ζάρι όταν είναι η σειρά του.\n\n" +
                "• Νικητής είναι ο πρώτος παίκτης που θα φέρει όλα τα πιόνια του στο τέρμα."
            );
            content.setWrapText(true);
            content.setEditable(false);
            content.setPrefSize(600, 400);

            VBox root = new VBox(10, title, content);
            root.setPadding(new Insets(10));

            Scene scene = new Scene(root);
            helpStage.setScene(scene);
            helpStage.showAndWait();
        });
        
        aboutItem.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Σχετικά");
            info.setHeaderText(null);
            info.setContentText(
                "Αυτό το παιχνίδι φτιάχτηκε από τους φοιτητές Γαλιώτο και Μορούζ στο πλαίσιο" + 
                "του μαθήματος Προηγμένα Θέματα Προγραμματισμού.\n");

            info.getDialogPane().setPrefSize(520, 240);
            info.getDialogPane().setMinSize(400, 180);
            info.showAndWait();
        });

        gameMenu.getItems().addAll(playGameItem, stopGameItem, exitItem);
        helpMenu.getItems().addAll(helpItem, aboutItem);
        menuBar.getMenus().addAll(gameMenu, helpMenu);

        return menuBar;
    }
}
