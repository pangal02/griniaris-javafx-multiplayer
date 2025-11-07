package gr.uop;

import java.util.ArrayList;

import gr.uop.logic.GameEngine;
import gr.uop.model.Player;
import gr.uop.scenes.LoginScene;
import gr.uop.scenes.SceneManager;
import javafx.application.Application;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * JavaFX App
 */

public class App extends Application {
    private static GameEngine gameEngine;
    private Color[] colors = new Color[] { Color.RED, Color.BLUE, Color.YELLOW, Color.LIME };

    @Override
    public void start(Stage stage) {
        SceneManager.setStage(stage);
        stage.setTitle("Γκρινιάρης - Client");
        SceneManager.changeScene(new LoginScene().create());
        stage.show();
}

    public static void main(String[] args) {
        launch(args);
    }

    public static void exit() {
        System.exit(0);
    }

}
