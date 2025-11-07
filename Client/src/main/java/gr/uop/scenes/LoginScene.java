package gr.uop.scenes;

import gr.uop.net.Client;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LoginScene {
    private int port;
    private Label errorLabel = new Label("");

    public Scene create() {
        Label title = new Label("Σύνδεση στον Γκρινιάρη");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Όνομα Παίκτη");

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Διεύθυνση IP");

        // Πεδίο για port
        Label portLabel = new Label("Θύρα:");
        TextField portField = new TextField("7777");
        portField.setPromptText("Port");

        Button connectBtn = new Button("Σύνδεση");
        connectBtn.setPrefWidth(200);

        connectBtn.setOnAction(event -> {
            String name = nameField.getText().trim();
            String ip = ipField.getText().trim();
            String portStr = portField.getText().trim();
            errorLabel.setStyle("-fx-text-fill: red;");

            if (name.isEmpty()) {
                errorLabel.setText("* Απαιτείται όνομα");
                return;
            }
            if (ip.isEmpty()) {
                errorLabel.setText("Απαιτείται διεύθυνση IP");
                return;
            }
            if (portStr.isEmpty()){
                errorLabel.setText("* Απαιτείται αριθμός θύρας!");
                return;
            }

            try {
                port = Integer.parseInt(portStr);
                // Δημιουργία Client (socket + game engine)
                Client client = new Client(ip, port, name);

                // Δημιουργία Lobby με το client
                LobbyScene lobby = new LobbyScene(client);

                // Αλλαγή σκηνής μέσω SceneManager
                SceneManager.changeScene(lobby.getScene());

            } 
            catch(NumberFormatException nfe){
                System.err.println("Error: " + nfe);
                errorLabel.setText("* Απαιτείται αριθμός port");
            } 
            catch (IOException e) {
                errorLabel.setText("Αποτυχία σύνδεσης: " + e.getMessage());
            }
        });

        VBox layout = new VBox(12, title, nameField, ipField, portField, connectBtn, errorLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));

        return new Scene(layout, 480, 320);
    }
}
