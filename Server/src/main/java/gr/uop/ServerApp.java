package gr.uop;

import gr.uop.server.GameServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ServerApp extends Application {

    private TextArea logArea;
    private Button startStopButton;
    private TextField portField;
    private Label statusLabel;

    private boolean isRunning = false;
    private GameServer server;

    @Override
    public void start(Stage stage) {
        Label title = new Label("🎮 Γκρινιάρης - Server");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label portLabel = new Label("Θύρα:");
        portField = new TextField("7777");
        portField.setPrefWidth(80);

        startStopButton = new Button("Start Server");
        startStopButton.setPrefWidth(150);
        startStopButton.setOnAction(e -> toggleServer());

        statusLabel = new Label("Server ανενεργός.");
        statusLabel.setStyle("-fx-text-fill: darkred;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(350);

        HBox topBox = new HBox(10, portLabel, portField, startStopButton);
        topBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, title, topBox, statusLabel, logArea);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 500, 480);
        stage.setScene(scene);
        stage.setTitle("Γκρινιάρης - Server");
        
        stage.setOnCloseRequest(event -> {
            if (isRunning) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Ο server είναι σε λειτουργία. Θέλετε να τον σταματήσετε και να κλείσετε την εφαρμογή;",
                        ButtonType.YES, ButtonType.NO);
                alert.setTitle("Τερματισμός server");
                alert.setHeaderText("Κλείσιμο διακομιστή");
                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.YES) {
                        stopServerAndResetUI();
                        Platform.exit();
                    } else {
                        event.consume();
                    }
                });
            } else {
                Platform.exit(); // ή άφησέ το κενό για default συμπεριφορά
            }
        });
        stage.show();
    }

    private void toggleServer() {
        if (!isRunning) {
            try {
                int port = Integer.parseInt(portField.getText());
                server = new GameServer(port, this::appendLog);
                server.start();

                startStopButton.setText("Stop Server");
                portField.setDisable(true);
                statusLabel.setText("✅ Server σε λειτουργία στη θύρα " + port);
                statusLabel.setStyle("-fx-text-fill: green;");
                isRunning = true;

            } catch (NumberFormatException e) {
                appendLog("❌ Μη έγκυρος αριθμός θύρας!");
            }
        } else {
            stopServerAndResetUI();
        }
    }

    private void stopServerAndResetUI() {
        server.stopServer();
        startStopButton.setText("Start Server");
        startStopButton.setDisable(false);
        portField.setDisable(false);
        statusLabel.setText("🛑 Server ανενεργός.");
        statusLabel.setStyle("-fx-text-fill: darkred;");
        isRunning = false;
    }

    public void appendLog(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
