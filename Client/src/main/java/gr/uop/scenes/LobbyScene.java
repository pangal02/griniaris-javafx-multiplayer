package gr.uop.scenes;

import gr.uop.model.Player;
import gr.uop.net.Client;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.ProgressIndicator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class LobbyScene {

    private final Client client;
    private final ObservableList<PlayerInfo> playerList = FXCollections.observableArrayList();
    private final Map<Integer, PlayerInfo> players = new ConcurrentHashMap<>();
    private volatile int hostId = -1;
    private volatile boolean allReadyAnnounced = false;

    private final ListView<PlayerInfo> listView = new ListView<>();
    private static final String READY_LABEL = "Ready";
    private static final String UNREADY_LABEL = "Unready";

    private final Button readyButton = new Button(READY_LABEL);
    private final Button startButton = new Button("🎮 Start Game");
    private final Label statusLabel = new Label("Περιμένουμε τους παίκτες...");

    // Προκαθορισμένα χρώματα
    private static final Map<String, Color> COLOR_MAP = Map.of(
            "RED", Color.RED,
            "BLUE", Color.BLUE,
            "YELLOW", Color.YELLOW,
            "GREEN", Color.LIME);
    private static final String HOST_START_STYLE = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String WAITING_START_STYLE = "-fx-background-color: #cccccc; -fx-text-fill: #666666;";

    public LobbyScene(Client client) {
        this.client = client;
        setupUI();
        setupListeners();
        
        // Τώρα που το callback είναι έτοιμο, στείλε JOIN
        client.send("JOIN " + client.getName());
        
        SceneManager.getStage().setTitle("Lobby - " + client.getName());
    }

    public Scene getScene() {
        VBox layout = new VBox(16,
                new Label("🎮 Λόμπι Παιχνιδιού"),
                listView,
                readyButton,
                startButton,
                statusLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setPrefSize(500, 450);
        return new Scene(layout);
    }

    // ------------------- UI -------------------

    private void setupUI() {
        listView.setItems(playerList);
        listView.setCellFactory(param -> new PlayerCell());
        listView.setPrefHeight(280);

        readyButton.setDisable(true);
        startButton.setDisable(true);
        startButton.setVisible(false);

        readyButton.setOnAction(e -> {
            PlayerInfo self = players.get(client.getPlayerId());
            if (self == null) {
                return;
            }
            boolean nextReady = !self.ready;
            client.send("READY " + client.getPlayerId() + " " + nextReady);
            readyButton.setText(nextReady ? UNREADY_LABEL : READY_LABEL);
        });
        
        startButton.setOnAction(e -> {
            if (client.getPlayerId() != hostId) {
                return;
            }
            // Disable αμέσως για να αποφύγουμε duplicate START commands
            startButton.setDisable(true);
            statusLabel.setText("🚀 Ξεκινάει το παιχνίδι...");
            
            // Στείλε START command στον server
            // Θα περιμένουμε το broadcast "START" από τον server για να αλλάξουμε scene
            client.send("START");
        });

        updateStartButtonUI();
    }

    // ------------------- Listeners -------------------

    private void setupListeners() {
        // Ο handler τώρα χειρίζεται το START ΕΞΩΤΕΡΙΚΑ του Platform.runLater
        // για να αποφύγουμε race condition με το επόμενο TURN message
        client.setMessageHandler(msg -> {
            if (msg.equals("START")) {
                // Δημιούργησε snapshot για thread-safety
                ArrayList<Player> gamePlayersList = new ArrayList<>();
                synchronized (players) {
                    for (PlayerInfo p : players.values()) {
                        gamePlayersList.add(new Player(p.id, p.name, p.color));
                    }
                }

                Platform.runLater(() -> SceneManager.changeScene(createLoadingContent()));

                CountDownLatch latch = new CountDownLatch(1);
                final GameScene[] holder = new GameScene[1];

                Platform.runLater(() -> {
                    client.getGameEngine().setPlayersList(gamePlayersList);
                    GameScene gameScene = new GameScene(client);
                    holder[0] = gameScene;
                    SceneManager.changeScene(gameScene.getScene());
                    gameScene.forceRebuildBoard();
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                final GameScene gameScene = holder[0];
                client.setMessageHandler(msg2 -> Platform.runLater(() -> gameScene.handleServerMessage(msg2)));
                System.out.println("🔄 Handler switched to GameScene"); // DEBUG
            } else {
                // Όλα τα άλλα messages πηγαίνουν στο handleServerMessage μέσω Platform.runLater
                Platform.runLater(() -> handleServerMessage(msg));
            }
        });
    }

    private void handleServerMessage(String msg) {
        System.out.println("📨 LobbyScene received: " + msg); // DEBUG
        if (msg.startsWith("PLAYER")) {
            // PLAYER <id> <name> <color> <ready>
            String[] p = msg.split(" ");
            int id = Integer.parseInt(p[1]);
            String name = p[2];
            String colorName = p[3];
            Color color = COLOR_MAP.getOrDefault(colorName.toUpperCase(), Color.GRAY);
            boolean ready = Boolean.parseBoolean(p[4]);
            players.put(id, new PlayerInfo(id, name, colorName, color, ready, id == hostId));
            refreshPlayerList();
        } 
        else if (msg.startsWith("ID")) {
            // ID <number>
            int id = Integer.parseInt(msg.split(" ")[1]);
            client.setPlayerId(id);
            System.out.println("Το ID μου είναι: " + id);
        }
        else if (msg.startsWith("COLOR")) {
            // COLOR <players_id> <color>
            String[] p = msg.split(" ");
            int id = Integer.parseInt(p[1]);
            String colorName = p[2];
            Color color = COLOR_MAP.getOrDefault(colorName.toUpperCase(), Color.GRAY);
            if (players.containsKey(id)) {
                players.get(id).colorName = colorName;
                players.get(id).color = color;
                refreshPlayerList();
            }
        } 
        else if (msg.startsWith("COLOR_DENIED")) {
            String denied = msg.split(" ")[1];
            showWarning("Το χρώμα " + denied + " είναι ήδη πιασμένο!");
        } 
        else if (msg.startsWith("READY")) {
            String[] p = msg.split(" ");
            int playerId = Integer.parseInt(p[1]);
            boolean ready = Boolean.parseBoolean(p[2]);
            if (players.containsKey(playerId)) {
                players.get(playerId).ready = ready;
            }
            if (!ready) {
                allReadyAnnounced = false;
            }
            refreshPlayerList();
        } else if (msg.startsWith("HOST")) {
            String[] p = msg.split(" ");
            hostId = Integer.parseInt(p[1]);
            refreshPlayerList();
        } else if (msg.startsWith("PLAYER_LEFT")) {
            String[] p = msg.split(" ");
            int id = Integer.parseInt(p[1]);
            players.remove(id);
            if (id == hostId) {
                hostId = -1;
            }
            allReadyAnnounced = false;
            refreshPlayerList();
        } else if (msg.equals("ALL_READY")) {
            statusLabel.setText("✅ Όλοι έτοιμοι! Πατήστε Start Game για να ξεκινήσετε.");
            allReadyAnnounced = true;
            updateStartButtonUI();
        } else if (msg.startsWith("START_DENIED")) {
            showWarning("Μόνο ο host μπορεί να ξεκινήσει το παιχνίδι.");
        }
        // ❌ ΑΦΑΙΡΕΘΗΚΕ: Το START τώρα χειρίζεται στο setupListeners()
    }

    private void refreshPlayerList() {
        List<PlayerInfo> sorted = new ArrayList<>(players.values());
        for (PlayerInfo info : sorted) {
            info.host = (info.id == hostId);
        }
        sorted.sort(Comparator.comparing((PlayerInfo p) -> !p.host).thenComparingInt(p -> p.id));
        playerList.setAll(sorted);
        boolean allReady = !sorted.isEmpty() && sorted.stream().allMatch(p -> p.ready);
        if (!allReady) {
            allReadyAnnounced = false;
        }
        statusLabel.setText(allReady ? "Όλοι έτοιμοι! Περιμένετε..." : "Περιμένουμε τους υπόλοιπους...");
        updateReadyButtonState();
        updateStartButtonUI();
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void updateReadyButtonState() {
        PlayerInfo self = players.get(client.getPlayerId());
        boolean hasColor = self != null && self.colorName != null && !self.colorName.equalsIgnoreCase("NONE");
        readyButton.setDisable(!hasColor);
        if (!hasColor) {
            readyButton.setText(READY_LABEL);
            return;
        }

        readyButton.setText(self.ready ? UNREADY_LABEL : READY_LABEL);
    }

    private void updateStartButtonUI() {
        if (!allReadyAnnounced) {
            startButton.setDisable(true);
            startButton.setVisible(false);
            startButton.setText("🎮 Start Game");
            startButton.setStyle("");
            return;
        }

        boolean isHost = client.getPlayerId() == hostId;
        startButton.setVisible(true);
        if (isHost) {
            startButton.setDisable(false);
            startButton.setText("🎮 Start Game");
            startButton.setStyle(HOST_START_STYLE);
        } else {
            startButton.setDisable(true);
            startButton.setText("🎮 Waiting for host to start...");
            startButton.setStyle(WAITING_START_STYLE);
        }
    }

    // ------------------- Εσωτερικές κλάσεις -------------------

    private static class PlayerInfo {
        int id;
        String name;
        String colorName;
        Color color;
        boolean ready;
        boolean host;

        PlayerInfo(int id, String name, String colorName, Color color, boolean ready, boolean host) {
            this.id = id;
            this.name = name;
            this.colorName = colorName;
            this.color = color;
            this.ready = ready;
            this.host = host;
        }
    }

    private Parent createLoadingContent() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(80, 80);
        Label label = new Label("🎲 Φόρτωση ταμπλό...");
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #333333;");
        box.getChildren().addAll(progress, label);
        box.setPrefSize(700, 600);
        return box;
    }

    /** Custom cell renderer με mini color picker */
    private class PlayerCell extends ListCell<PlayerInfo> {
        @Override
        protected void updateItem(PlayerInfo player, boolean empty) {
            super.updateItem(player, empty);
            if (empty || player == null) {
                setGraphic(null);
                return;
            }

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Circle colorCircle = new Circle(10, player.color);
            Label name = new Label(player.host ? player.name + " (Host)" : player.name);
            Label readyLabel = new Label(player.ready ? "✅ Ready" : "⏳ Waiting...");

            // Αν είναι ο εαυτός μας, δείξε επιλογή χρώματος
            if (player.id == client.getPlayerId()) {
                Button colorButton = new Button();
                colorButton.setMinSize(24, 24);
                colorButton.setMaxSize(24, 24);
                colorButton.setStyle("-fx-background-color: " + player.colorName.toLowerCase() + ";");

                ContextMenu colorMenu = new ContextMenu();

                // Μόνο διαθέσιμα χρώματα
                Set<String> usedColors = new HashSet<>();
                players.values().forEach(p -> usedColors.add(p.colorName.toUpperCase()));

                for (Map.Entry<String, Color> entry : COLOR_MAP.entrySet()) {
                    String colorName = entry.getKey();
                    Color color = entry.getValue();

                    if (usedColors.contains(colorName) && !colorName.equals(player.colorName))
                        continue;

                    MenuItem item = new MenuItem(colorName);
                    Circle icon = new Circle(7, color);
                    item.setGraphic(icon);
                    item.setOnAction(ev -> {
                        client.send("COLOR " + client.getPlayerId() + " " + colorName);
                        colorButton.setStyle("-fx-background-color: " + colorName.toLowerCase() + ";");
                    });
                    colorMenu.getItems().add(item);
                }

                colorButton.setOnMouseClicked(e -> colorMenu.show(colorButton, e.getScreenX(), e.getScreenY()));

                row.getChildren().addAll(colorButton, name, readyLabel);
            } else {
                row.getChildren().addAll(colorCircle, name, readyLabel);
            }

            setGraphic(row);
        }
    }
}
