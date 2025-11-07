package gr.uop.scenes;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage primaryStage;
    private static final BorderPane rootLayout = new BorderPane(); // final: ένα και μοναδικό
    private static Scene appScene;

    public static void setStage(Stage stage) {
        primaryStage = stage;
        rootLayout.setTop(MenuFactory.createMenuBar());  // μόνιμο MenuBar

        appScene = new Scene(rootLayout, 900, 900);
        primaryStage.setScene(appScene);
        primaryStage.show();
    }

    // Δέχεται Scene και βάζει μόνο το root στο center, το MenuBar μένει
    public static void changeScene(Scene scene) {
        rootLayout.setCenter(scene.getRoot());
        appScene.getStylesheets().setAll(scene.getStylesheets());
    }

    // Εναλλακτικά, απευθείας Parent
    public static void changeScene(Parent content) {
        rootLayout.setCenter(content);
    }

    public static Stage getStage(){
        return primaryStage;
    }

    public static Scene getAppScene() {
        return appScene;
    }
}