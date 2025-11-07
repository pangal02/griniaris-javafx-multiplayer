package gr.uop.scenes;

import gr.uop.net.Client;
import gr.uop.logic.Move;
import gr.uop.model.BoardSquare;
import gr.uop.model.HomeSquare;
import gr.uop.model.HomeSquare;
import gr.uop.model.Player;
import gr.uop.model.Pawn;
import gr.uop.model.ShieldSquare;
import gr.uop.model.StarSquare;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Cursor;
import javafx.scene.Group;

public class GameScene {

    private final String FONT_FAMILY = "System";
    private final int BOARD_SIZE = 15; // 15x15 grid
    private final int CELL_SIZE = (int) (30 * 1.5);
    private Client client;
    private ArrayList<Player> players;
    private Player myPlayer;

    private BorderPane root;
    private GridPane boardGrid;
    private Label currentPlayerLabel;
    private Label diceLabel;
    private Button rollButton;
    private Button passButton;
    private Label messageLabel;
    private List<Player> playersRef;
    private final Map<BoardSquare, Pane> cellMap = new HashMap<>();
    private final Set<BoardSquare> validMoveSquares = new HashSet<>();
    private Integer lastDice = null;
    private Integer diceValue = null;
    private Pane previewDestPane = null;
    private boolean usedSixExtra = false;
    private Pawn selectedPawn = null;
    private BoardSquare selectedDestination = null;
    private BoardSquare primaryDestination = null;
    private BoardSquare finalDestination = null;
    private Pane selectedSourcePane = null;
    private Pane selectedDestinationPane = null;
    private final List<BoardSquare> highlightedDestinations = new ArrayList<>();

    private static final String DEFAULT_BORDER_STYLE = "-fx-border-color: grey; -fx-border-width: 1;";
    private static final String VALID_MOVE_STYLE = "-fx-border-color: gold; -fx-border-width: 3;";
    private static final String SELECTED_PAWN_STYLE = "-fx-border-color: #ff9800; -fx-border-width: 4;";
    private static final String DESTINATION_STYLE = "-fx-border-color: #ff5722; -fx-border-width: 4;";
    private static final String INTERMEDIATE_STYLE = "-fx-border-color: #FFB74D; -fx-border-width: 4;";

    public GameScene(Client client) {
        this.client = client;
        this.myPlayer = client.getGameEngine().getPlayerById(client.getPlayerId());
        this.root = new BorderPane();
        this.root.setPadding(new Insets(12));
        this.root.setStyle("-fx-background-color: #f5f5f5;");

        buildUI();
        Platform.runLater(() -> SceneManager.getStage().setTitle("Game - " + client.getName()));
    }

    public Scene getScene() {
        return new Scene(root, 800, 800);
    }

    // ------------------- UI -------------------

    private void buildUI() {
        diceLabel = new Label("–");
        currentPlayerLabel = new Label("Παίζει: –");

        rollButton = new Button("Ρίξε");
        rollButton.setDisable(true);
        rollButton.setOnAction(e -> {
            int val = client.getGameEngine().rollDice();
            client.send("ROLL " + myPlayer.getId() + " " + val);
        });

        passButton = new Button("Πάσο");
        passButton.setDisable(true);
        passButton.setOnAction(e -> {
            System.out.println("just clicked on bass");
            client.send("PASS " + myPlayer.getId());
            System.out.println("sent pass message to server");
        });

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.setTextFill(Color.web("#d32f2f"));

        HBox dicePanel = new HBox(16, currentPlayerLabel,
                new Label("Ζάρι:"), diceLabel,
                rollButton, passButton);
        dicePanel.setAlignment(Pos.CENTER);
        dicePanel.setPadding(new Insets(8));

        VBox bottomBox = new VBox(8, dicePanel, messageLabel);
        bottomBox.setAlignment(Pos.CENTER);
        root.setBottom(bottomBox);
    }

    private void updateTurnUI(Player current) {
        if (current == null) {
            currentPlayerLabel.setText("Παίζει: –");
            rollButton.setDisable(true);
            passButton.setDisable(true);
            return;
        }

        // Ενημέρωση του label με το όνομα του παίκτη
        currentPlayerLabel.setText("Παίζει: " + current.getName());

        // Debug
        System.out.println(
                "🔄 updateTurnUI: Current player = " + current.getName() + ", My player = " + myPlayer.getName());
        System.out.println("🔄 isMyTurn() = " + isMyTurn());

        // Χρώμα δίπλα στο όνομα
        Circle colorIndicator = new Circle(6);
        colorIndicator.setFill(current.getColor());
        currentPlayerLabel.setGraphic(colorIndicator);
        currentPlayerLabel.setContentDisplay(ContentDisplay.RIGHT);

        // Ενεργοποίηση κουμπιών βάσει σειράς
        boolean myTurn = isMyTurn();
        if (myTurn) {
            // Είναι η σειρά μου - ενεργοποίησε μόνο το roll
            rollButton.setDisable(false);
            passButton.setDisable(true); // Το pass ενεργοποιείται μετά το roll
            System.out.println("✅ Ενεργοποιήθηκε το rollButton για " + myPlayer.getName());
        } else {
            // Δεν είναι η σειρά μου - απενεργοποίησε όλα
            rollButton.setDisable(true);
            passButton.setDisable(true);
            System.out.println("❌ Απενεργοποιήθηκαν τα κουμπιά - δεν είναι η σειρά του " + myPlayer.getName());
        }
    }

    // ------------------- Server Listener -------------------

    // Package-private για να μπορεί το LobbyScene να το καλέσει
    void handleServerMessage(String msg) {
        System.out.println("📨 GameScene received: " + msg); // DEBUG

        // ✅ ΚΡΙΤΙΚΟ: ΟΛΑ τα UI updates πρέπει να τρέχουν στο JavaFX thread!
        if (msg.startsWith("ROLL")) {
            handleRoll(msg);
        } else if (msg.startsWith("MOVE")) {
            handleMove(msg);
        } else if (msg.startsWith("PASS")) {
            handlePass(msg);
        } else if (msg.startsWith("TURN")) {
            handleTurn(msg);
        } else if (msg.startsWith("GAME_ABORTED")) {
            showGameAborted();
        } else if (msg.startsWith("SERVER_SHUTDOWN")) {
            showServerShutdown();
        }
    }

    private void handleRoll(String msg) {
        // ROLL <playerId> <value>
        String[] parts = msg.split(" ");
        int playerId = Integer.parseInt(parts[1]);
        int val = Integer.parseInt(parts[2]);

        // Όλοι βλέπουν το αποτέλεσμα του ζαριού
        diceLabel.setText(String.valueOf(val));
        diceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        clearMessage();

        // Βρες ποιος παίκτης έριξε το ζάρι
        Player roller = client.getGameEngine().getPlayerById(playerId);
        if (roller != null) {
            System.out.println("🎲 " + roller.getName() + " έριξε " + val);
        }

        // Αν είναι η σειρά μου, ενεργοποίησε τα κουμπιά
        if (playerId == myPlayer.getId()) {
            lastDice = val;
            highlightValidMoves(val);

            // Έλεγξε αν υπάρχουν έγκυρες κινήσεις
            List<Pawn> validMoves = client.getGameEngine().getValidMoves(val);
            if (validMoves.isEmpty()) {
                // Δεν υπάρχουν κινήσεις, ενεργοποίησε μόνο το pass
                passButton.setDisable(false);
                rollButton.setDisable(true);
                System.out.println("⚠️ Δεν υπάρχουν έγκυρες κινήσεις - πρέπει να κάνεις pass");
                showInfoMessage("Δεν υπάρχουν έγκυρες κινήσεις. Πρέπει να κάνεις πάσο.");
            } else {
                passButton.setDisable(false);
                rollButton.setDisable(true);
            }
        } else {
            // Αν δεν είναι η σειρά μου, απενεργοποίησε όλα
            clearHighlights();
            rollButton.setDisable(true);
            passButton.setDisable(true);
        }
    }

    private void handleMove(String msg) {
        // MOVE <playerId> <pawnId> <toId>
        String[] parts = msg.split(" ");
        int playerId = Integer.parseInt(parts[1]);
        int pawnId = Integer.parseInt(parts[2]);
        int toId = Integer.parseInt(parts[3]);

        client.getGameEngine().applyRemoteMove(playerId, pawnId, toId);

        // ✅ REBUILD το board για να δείξει τη νέα θέση
        rebuildBoard();


        // Καθάρισε highlights και dice μετά τη κίνηση για όλους
        clearHighlights();
        lastDice = null;
        clearMessage();
        if (client.getGameEngine().hasWin(client.getGameEngine().getPlayerById(playerId))) {
            System.out.println("🎉 Ο παίκτης " + client.getGameEngine().getPlayerById(playerId).getName() + " κέρδισε!");
            showInfoMessage("🎉 Ο παίκτης " + client.getGameEngine().getPlayerById(playerId).getName() + " κέρδισε!");
            
        }
    }

    private void handlePass(String msg) {
        // PASS <playerId>
        String[] parts = msg.split(" ");
        int playerId = Integer.parseInt(parts[1]);
        Player player = client.getGameEngine().getPlayerById(playerId);
        System.out.println("Player " + player.getName() + " passed.");

        // Καθάρισε highlights και dice για όλους
        clearHighlights();
        lastDice = null;
        clearMessage();

        // Απενεργοποίησε κουμπιά μετά το pass
        if (playerId == myPlayer.getId()) {
            rollButton.setDisable(true);
            passButton.setDisable(true);
        }
    }

    private void handleTurn(String msg) {
        // TURN <playerId>
        String[] parts = msg.split(" ");
        int playerId = Integer.parseInt(parts[1]);

        System.out.println("📨 Received TURN message for playerId: " + playerId);
        System.out.println("📨 My player ID: " + myPlayer.getId());

        Player current = client.getGameEngine().getPlayerById(playerId);
        if (current == null) {
            System.err.println("❌ ERROR: Could not find player with ID: " + playerId);
            throw new IllegalStateException("Invalid player ID in TURN message: " + playerId);
        }

        System.out.println("📨 Found player: " + current.getName() + " (id=" + current.getId() + ")");

        client.getGameEngine().setCurrentPlayer(current);

        // Καθάρισε highlights από προηγούμενη σειρά
        clearHighlights();
        lastDice = null;
        clearMessage();


        // Ενημέρωσε UI για τη νέα σειρά
        updateTurnUI(current);
    }

    // ------------------- Board Rendering -------------------

    // Public μέθοδος για force rebuild από το LobbyScene
    public void forceRebuildBoard() {
        if (!cellMap.isEmpty()) {
            System.out.println("⚠️ forceRebuildBoard() called but board already exists, skipping");
            return;
        }
        System.out.println("🏗️ forceRebuildBoard() called from LobbyScene");
        rebuildBoard();
    }

    private void rebuildBoard() {
        System.out.println("🏗️ rebuildBoard() START"); // DEBUG
        cellMap.clear();
        GridPane gp = new GridPane();
        // gp.setHgap(0);
        // gp.setVgap(0);
        gp.setAlignment(Pos.CENTER);
        gp.setGridLinesVisible(true);

        gp.add(cornerStartCell(Color.LIME, Math.pow(BOARD_SIZE, 2)), 0, 0);
        gp.add(topCells(CELL_SIZE), 1, 0);
        gp.add(cornerStartCell(Color.RED, Math.pow(BOARD_SIZE, 2)), 2, 0);

        gp.add(leftCells(CELL_SIZE), 0, 1);
        gp.add(centerCell(Math.pow(BOARD_SIZE, 2)), 1, 1);
        gp.add(rightCells(CELL_SIZE), 2, 1);

        gp.add(cornerStartCell(Color.YELLOW, Math.pow(BOARD_SIZE, 2)), 0, 2);
        gp.add(bottomCells(CELL_SIZE), 1, 2);
        gp.add(cornerStartCell(Color.BLUE, Math.pow(BOARD_SIZE, 2)), 2, 2);

        boardGrid = gp;
        System.out.println("✅ rebuildBoard() created grid with " + cellMap.size() + " cells");
        root.setCenter(boardGrid);
        System.out.println("✅ root.setCenter(boardGrid) completed");

    }

    


    private Pane cornerStartCell(Color color, double size) {
        List<BoardSquare> baseSquares = client.getGameEngine().getBoard().getBaseCellsFor(color);

        StackPane cornerPane = new StackPane();
        Rectangle bg = new Rectangle(size, size, color);
        Polygon whitePolygon = new Polygon(
                0, size / 2, size / 2, 0,
                size, size / 2,
                size / 2, size);
        whitePolygon.setFill(Color.WHITE);
        whitePolygon.setStroke(Color.GRAY);
        whitePolygon.setStrokeWidth(1);

        Polygon borderPolygon = new Polygon(
                size * 0.1, size / 2, size / 2, size * 0.1,
                size * 0.9, size / 2,
                size / 2, size * 0.9);
        borderPolygon.setFill(Color.TRANSPARENT);
        borderPolygon.setStroke(Color.GRAY);
        borderPolygon.setStrokeWidth(2);

        // 4 BaseSquare cells γύρω από το κέντρο (σταυρός), κεντρικό λευκό
        double cellSize = size / 6; // ένα κελί αναλογικά με τη γωνία
        StackPane cross = new StackPane();

        Node upCell = cellNode(baseSquares.get(0), cellSize);
        upCell.setTranslateY(-cellSize);

        Node rightCell = cellNode(baseSquares.get(1), cellSize);
        rightCell.setTranslateX(cellSize);

        Node downCell = cellNode(baseSquares.get(2), cellSize);
        downCell.setTranslateY(cellSize);

        Node leftCell = cellNode(baseSquares.get(3), cellSize);
        leftCell.setTranslateX(-cellSize);

        Rectangle center = new Rectangle(cellSize, cellSize, Color.WHITE);
        center.setStroke(Color.GRAY);
        center.setStrokeWidth(1);

        cross.getChildren().addAll(upCell, rightCell, downCell, leftCell, center);

        cornerPane.getChildren().addAll(bg, whitePolygon, borderPolygon, cross);

        return cornerPane;
    }

    private Pane centerCell(double size) {
        Pane root = new Pane();
        HomeSquare redHomeSquare = (HomeSquare) client.getGameEngine().getBoard().getHomeSquaresFor(Color.RED).get(0);
        HomeSquare blueHomeSquare = (HomeSquare) client.getGameEngine().getBoard().getHomeSquaresFor(Color.BLUE).get(0);
        HomeSquare yellowHomeSquare = (HomeSquare) client.getGameEngine().getBoard().getHomeSquaresFor(Color.YELLOW)
                .get(0);
        HomeSquare greenHomeSquare = (HomeSquare) client.getGameEngine().getBoard().getHomeSquaresFor(Color.LIME)
                .get(0);

        // 4 τριγωνα που κοιτάζουν προς το κέντρο (backgrounds)
        Polygon topTriangle = new Polygon(
                0, 0, // αριστερή βάση
                size, 0, // δεξιά βάση
                size / 2, size / 2 // κορυφή προς το κέντρο
        );
        topTriangle.setFill(Color.RED);
        topTriangle.setStroke(Color.GRAY);
        topTriangle.setStrokeWidth(1);

        Polygon rightTriangle = new Polygon(
                size, 0, // πάνω βάση
                size, size, // κάτω βάση
                size / 2, size / 2 // κορυφή προς το κέντρο
        );
        rightTriangle.setFill(Color.BLUE);
        rightTriangle.setStroke(Color.GRAY);
        rightTriangle.setStrokeWidth(1);

        Polygon bottomTriangle = new Polygon(
                0, size, // αριστερή βάση
                size, size, // δεξιά βάση
                size / 2, size / 2 // κορυφή προς το κέντρο
        );
        bottomTriangle.setFill(Color.YELLOW);
        bottomTriangle.setStroke(Color.GRAY);
        bottomTriangle.setStrokeWidth(1);

        Polygon leftTriangle = new Polygon(
                0, 0, // πάνω βάση
                0, size, // κάτω βάση
                size / 2, size / 2 // κορυφή προς το κέντρο
        );
        leftTriangle.setFill(Color.LIME);
        leftTriangle.setStroke(Color.GRAY);
        leftTriangle.setStrokeWidth(1);

        // Δημιουργία clickable HomeSquare cells (μικρά τρίγωνα μέσα στα μεγάλα)
        double cellSize = size / 5; // Μέγεθος του HomeSquare cell

        // RED HomeSquare (πάνω τρίγωνο, κέντρο προς κάτω)
        StackPane redHomeCell = createHomeSquareCell(redHomeSquare, cellSize);
        redHomeCell.setLayoutX(size / 2 - cellSize / 2);
        redHomeCell.setLayoutY(size / 6);

        // BLUE HomeSquare (δεξί τρίγωνο, κέντρο προς αριστερά)
        StackPane blueHomeCell = createHomeSquareCell(blueHomeSquare, cellSize);
        blueHomeCell.setLayoutX(size - size / 6 - cellSize);
        blueHomeCell.setLayoutY(size / 2 - cellSize / 2);

        // YELLOW HomeSquare (κάτω τρίγωνο, κέντρο προς πάνω)
        StackPane yellowHomeCell = createHomeSquareCell(yellowHomeSquare, cellSize);
        yellowHomeCell.setLayoutX(size / 2 - cellSize / 2);
        yellowHomeCell.setLayoutY(size - size / 6 - cellSize);

        // GREEN HomeSquare (αριστερό τρίγωνο, κέντρο προς δεξιά)
        StackPane greenHomeCell = createHomeSquareCell(greenHomeSquare, cellSize);
        greenHomeCell.setLayoutX(size / 6);
        greenHomeCell.setLayoutY(size / 2 - cellSize / 2);

        // Προσθήκη όλων στο root
        root.getChildren().addAll(
                topTriangle, rightTriangle, bottomTriangle, leftTriangle,
                redHomeCell, blueHomeCell, yellowHomeCell, greenHomeCell);

        root.setPrefSize(size, size);
        root.setMinSize(size, size);
        root.setMaxSize(size, size);
        return root;
    }

    /**
     * Δημιουργεί ένα clickable HomeSquare cell (τρίγωνο με pawns και event
     * handlers)
     */
    private StackPane createHomeSquareCell(HomeSquare homeSquare, double size) {
        StackPane cell = new StackPane();
        cell.setPrefSize(size, size);
        cell.setMinSize(size, size);
        cell.setMaxSize(size, size);

        // Τρίγωνο background με το χρώμα του HomeSquare (πιο σκούρο)
        Polygon triangle = new Polygon(
                size / 2, 0, // κορυφή
                0, size, // αριστερή βάση
                size, size // δεξιά βάση
        );
        triangle.setFill(homeSquare.getColor().darker());
        triangle.setStroke(Color.BLACK);
        triangle.setStrokeWidth(2);

        cell.getChildren().add(triangle);

        // Προσθήκη στο cellMap για interaction
        cellMap.put(homeSquare, cell);
        cell.setStyle(DEFAULT_BORDER_STYLE);

        // Πιόνια που μπορεί να βρίσκονται στο HomeSquare
        Pane pawnsOverlay = createPawnsOverlay(homeSquare, size);
        if (pawnsOverlay != null) {
            cell.getChildren().add(pawnsOverlay);
        }

        // Label για το ID (optional - για debugging)
        Label idLabel = new Label(homeSquare.getId() + "");
        idLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: white;");
        idLabel.setMouseTransparent(true);
        cell.getChildren().add(idLabel);

        // Event handlers για click
        CellOnMouseEvent cellEvent = new CellOnMouseEvent(homeSquare);
        if (homeSquare.getId() == client.getGameEngine().getBoard().getHomeSquaresFor(myPlayer.getColor()).get(0).getId()) {

            cell.setOnMouseClicked(e -> {

                cellEvent.handle(e);

                System.out.println("🏠 Clicked HomeSquare id=" + homeSquare.getId());
            });
            cell.setOnMouseEntered(e -> {
                cell.setCursor(Cursor.HAND);
            });
            cell.setOnMouseExited(e -> {
                cell.setCursor(Cursor.DEFAULT);
            });
        }
        return cell;
    }

    private Pane topCells(double size) {
        GridPane topCells = new GridPane();
        topCells.setHgap(0);
        topCells.setVgap(0);
        topCells.setAlignment(Pos.BOTTOM_CENTER);

        List<BoardSquare> squares = client.getGameEngine().getBoard().getMainPath();
        int front = 0;
        int rear = 4;
        while (front <= rear) {
            BoardSquare sq = squares.get(front);
            topCells.add(cellNode(sq, size), 2, front + 1); // Χρήση ξεχωριστής μεθόδου
            front++;
        }
        front = 44;
        rear = 49;
        while (front <= rear) {
            BoardSquare sq = squares.get(front);
            topCells.add(cellNode(sq, size), 0, rear - front); // Χρήση ξεχωριστής μεθόδου
            front++;
        }
        topCells.add(cellNode(squares.get(50), size), 1, 0); // Χρήση ξεχωριστής μεθόδου
        topCells.add(cellNode(squares.get(51), size), 2, 0); // Χρήση ξεχωριστής μεθόδου

        List<BoardSquare> redFinals = client.getGameEngine().getBoard().getFinalRowFor(Color.RED);
        for (int i = 0; i < redFinals.size(); i++) {
            topCells.add(cellNode(redFinals.get(i), size), 1, i + 1); // Χρήση ξεχωριστής μεθόδου
        }

        return topCells;
    }

    private Pane rightCells(double size) {
        GridPane rightCells = new GridPane();
        rightCells.setHgap(0);
        rightCells.setVgap(0);
        rightCells.setAlignment(Pos.CENTER_LEFT);

        rightCells.setGridLinesVisible(true);

        List<BoardSquare> squares = client.getGameEngine().getBoard().getMainPath();
        int front = 5;
        int rear = 10;
        while (front <= rear) {
            BoardSquare sq = squares.get(front);
            rightCells.add(cellNode(sq, size), front - 5, 0);
            front++;
        }
        rightCells.add(cellNode(squares.get(11), size), 5, 1);
        rightCells.add(cellNode(squares.get(12), size), 5, 2);
        rightCells.add(cellNode(squares.get(13), size), 4, 2);
        rightCells.add(cellNode(squares.get(14), size), 3, 2);
        rightCells.add(cellNode(squares.get(15), size), 2, 2);
        rightCells.add(cellNode(squares.get(16), size), 1, 2);
        rightCells.add(cellNode(squares.get(17), size), 0, 2);

        List<BoardSquare> blueFinals = client.getGameEngine().getBoard().getFinalRowFor(Color.BLUE);
        rightCells.add(cellNode(blueFinals.get(4), size), 0, 1);
        rightCells.add(cellNode(blueFinals.get(3), size), 1, 1);
        rightCells.add(cellNode(blueFinals.get(2), size), 2, 1);
        rightCells.add(cellNode(blueFinals.get(1), size), 3, 1);
        rightCells.add(cellNode(blueFinals.get(0), size), 4, 1);

        return rightCells;
    }

    private Pane bottomCells(double size) {
        GridPane bottomCells = new GridPane();
        bottomCells.setHgap(0);
        bottomCells.setVgap(0);
        bottomCells.setAlignment(Pos.TOP_CENTER);

        List<BoardSquare> squares = client.getGameEngine().getBoard().getMainPath();
        int front = 18;
        int rear = 23;
        while (front <= rear) {
            BoardSquare sq = squares.get(front);
            bottomCells.add(cellNode(sq, size), 2, front - 18);
            front++;
        }
        bottomCells.add(cellNode(squares.get(24), size), 1, 5);
        front = 25;
        rear = 30;
        while (front <= rear) {
            BoardSquare sq = squares.get(front);
            bottomCells.add(cellNode(sq, size), 0, 30 - front);
            front++;
        }

        List<BoardSquare> yellowFinals = client.getGameEngine().getBoard().getFinalRowFor(Color.YELLOW);
        bottomCells.add(cellNode(yellowFinals.get(4), size), 1, 0);
        bottomCells.add(cellNode(yellowFinals.get(3), size), 1, 1);
        bottomCells.add(cellNode(yellowFinals.get(2), size), 1, 2);
        bottomCells.add(cellNode(yellowFinals.get(1), size), 1, 3);
        bottomCells.add(cellNode(yellowFinals.get(0), size), 1, 4);
        return bottomCells;
    }

    private Pane leftCells(double size) {
        GridPane leftCells = new GridPane();
        leftCells.setHgap(0);
        leftCells.setVgap(0);
        leftCells.setAlignment(Pos.CENTER_RIGHT);

        List<BoardSquare> squares = client.getGameEngine().getBoard().getMainPath();
        leftCells.add(cellNode(squares.get(31), size), 5, 2);
        leftCells.add(cellNode(squares.get(32), size), 4, 2);
        leftCells.add(cellNode(squares.get(33), size), 3, 2);
        leftCells.add(cellNode(squares.get(34), size), 2, 2);
        leftCells.add(cellNode(squares.get(35), size), 1, 2);
        leftCells.add(cellNode(squares.get(36), size), 0, 2);
        leftCells.add(cellNode(squares.get(37), size), 0, 1);
        leftCells.add(cellNode(squares.get(38), size), 0, 0);
        leftCells.add(cellNode(squares.get(39), size), 1, 0);
        leftCells.add(cellNode(squares.get(40), size), 2, 0);
        leftCells.add(cellNode(squares.get(41), size), 3, 0);
        leftCells.add(cellNode(squares.get(42), size), 4, 0);
        leftCells.add(cellNode(squares.get(43), size), 5, 0);

        List<BoardSquare> greenFinals = client.getGameEngine().getBoard().getFinalRowFor(Color.LIME);
        for (int i = 0; i < greenFinals.size(); i++) {
            leftCells.add(cellNode(greenFinals.get(i), size), i + 1, 1); // Χρήση ξεχωριστής μεθόδου
        }

        return leftCells;
    }

    private Node cellNode(BoardSquare square, double size) {
        StackPane layoutCell = new StackPane();
        layoutCell.setPrefSize(size, size);
        layoutCell.setMinSize(size, size);
        layoutCell.setMaxSize(size, size);

        CellOnMouseEvent cellEvent = new CellOnMouseEvent(square);

        Rectangle rect = new Rectangle(size, size, square.getColor()); // Μεγαλύτερα cells
        rect.setStroke(Color.GRAY);
        rect.setStrokeWidth(1);

        layoutCell.getChildren().add(rect);
        cellMap.put(square, layoutCell);
        layoutCell.setStyle(DEFAULT_BORDER_STYLE);

        if (square instanceof StarSquare) {
            // Προσθήκη αστεριού στο κέντρο
            Polygon star = new Polygon();
            double centerX = size / 2.0;
            double centerY = size / 2.0;
            double radiusOuter = size * 0.35;
            double radiusInner = radiusOuter * 0.45;
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 5 * i - Math.PI / 2;
                double radius = (i % 2 == 0) ? radiusOuter : radiusInner;
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                star.getPoints().addAll(x, y);
            }
            star.setFill(Color.GOLD);
            star.setStroke(Color.BLACK);
            star.setStrokeWidth(1);

            layoutCell.getChildren().add(star);
        } else if (square instanceof ShieldSquare) {
            // Προσθήκη κύκλου στο κέντρο
            Circle circle = new Circle(size * 0.3, Color.VIOLET);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(1);

            layoutCell.getChildren().add(circle);
        }

        // Πιόνια πάνω στο κελί (αν υπάρχουν)
        Pane pawns = createPawnsOverlay(square, size);
        if (pawns != null) {
            layoutCell.getChildren().add(pawns);
        }

        Label idLabel = new Label(String.valueOf(square.getId()));
        idLabel.setMouseTransparent(true);
        layoutCell.getChildren().add(idLabel);
        layoutCell.setOnMouseClicked(cellEvent);
        layoutCell.setOnMouseEntered(cellEvent);
        layoutCell.setOnMouseExited(cellEvent);
        return layoutCell;
    }

    private void clearHighlights() {
        for (Pane p : cellMap.values()) {
            p.setStyle(DEFAULT_BORDER_STYLE);
        }
        validMoveSquares.clear();
        clearSelectionState();
        previewDestPane = null;
    }

    private void highlightValidMoves(int valueOfDice) {
        clearHighlights();
        if (valueOfDice <= 0)
            return;
        var engine = client.getGameEngine();
        var valid = engine.getValidMoves(valueOfDice);
        validMoveSquares.clear();
        for (Pawn pawn : valid) {
            BoardSquare sq = pawn.getPosition();
            Pane pane = cellMap.get(sq);
            if (pane != null) {
                validMoveSquares.add(sq);
                pane.setStyle(VALID_MOVE_STYLE);
            }
        }
    }

    private Pane createPawnsOverlay(BoardSquare square, double size) {
        List<Pawn> pawns = square.getPawns();
        if (pawns == null || pawns.isEmpty())
            return null;

        if (pawns.size() == 1) {
            StackPane sp = pawnNode(pawns.get(0), size * 0.28);
            sp.setPrefSize(size, size);
            return sp;
        }

        if (pawns.size() == 2) {
            HBox hb = new HBox(Math.max(2, size * 0.12));
            hb.setAlignment(Pos.CENTER);
            hb.setPickOnBounds(false);
            hb.getChildren().addAll(
                    pawnNode(pawns.get(0), size * 0.22),
                    pawnNode(pawns.get(1), size * 0.22));
            hb.setPrefSize(size, size);
            return hb;
        }

        // 3 ή περισσότερα: 2x2 grid, μέχρι 4 δείχνουμε, αν >4 εμφανίζουμε +N
        GridPane gp = new GridPane();
        gp.setHgap(Math.max(2, size * 0.08));
        gp.setVgap(Math.max(2, size * 0.08));
        gp.setAlignment(Pos.CENTER);
        int shown = Math.min(4, pawns.size());
        for (int i = 0; i < shown; i++) {
            gp.add(pawnNode(pawns.get(i), size * 0.18), i % 2, i / 2);
        }
        if (pawns.size() > 4) {
            Label more = new Label("+" + (pawns.size() - 4));
            more.setStyle("-fx-font-size: " + Math.max(10, size * 0.25));
            StackPane sp = new StackPane(gp, more);
            StackPane.setAlignment(more, Pos.BOTTOM_RIGHT);
            sp.setPrefSize(size, size);
            return sp;
        }
        gp.setPrefSize(size, size);
        return gp;
    }

    private Circle pawnCircle(Pawn pawn, double radius) {
        Circle circle = new Circle(radius, pawn.getColor() != null ? pawn.getColor().darker() : Color.GRAY);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(1.2);
        return circle;
    }

    private boolean isMyTurn() {
        Player current = client.getGameEngine().getCurrentPlayer();
        boolean result = current != null && current.equals(myPlayer);
        return result;
    }

    private void showErrorMessage(String text) {
        if (messageLabel == null) {
            return;
        }
        Platform.runLater(() -> {
            messageLabel.setTextFill(Color.web("#d32f2f"));
            messageLabel.setText(text);
            messageLabel.setVisible(true);
        });
    }

    private void clearMessage() {
        if (messageLabel == null) {
            return;
        }
        Platform.runLater(() -> {
            messageLabel.setText("");
            messageLabel.setVisible(false);
        });
    }

    private void showInfoMessage(String text) {
        if (messageLabel == null) {
            return;
        }
        Platform.runLater(() -> {
            messageLabel.setTextFill(Color.web("#1976d2"));
            messageLabel.setText(text);
            messageLabel.setVisible(true);
        });
    }

    private void showGameAborted() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING,
                "Το παιχνίδι ακυρώθηκε επειδή αποσυνδέθηκε παίκτης!",
                javafx.scene.control.ButtonType.OK);
        alert.setHeaderText("Παιχνίδι Ακυρώθηκε");
        alert.showAndWait();
        SceneManager.changeScene(new LobbyScene(client).getScene());
    }

    private void showServerShutdown() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "Ο server έκλεισε!",
                javafx.scene.control.ButtonType.OK);
        alert.setHeaderText("Διακοπή Σύνδεσης");
        alert.showAndWait();
        gr.uop.App.exit();
    }

    // ------------------- Cell Events -------------------

    private class CellOnMouseEvent implements EventHandler<MouseEvent> {
        private final BoardSquare square;
        private final Pawn pawn;

        public CellOnMouseEvent(BoardSquare square) {
            this(square, null);
        }

        public CellOnMouseEvent(BoardSquare square, Pawn pawn) {
            this.square = square;
            this.pawn = pawn;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getEventType() != MouseEvent.MOUSE_CLICKED) {
                return;
            }

            if (!isMyTurn()) {
                return;
            }

            if (pawn != null) {
                event.consume();
                handlePawnSelection(pawn);
                return;
            }

            if (lastDice == null) {
                return;
            }

            if (square != null && highlightedDestinations.contains(square)) {
                event.consume();
                if (primaryDestination != null && square.equals(primaryDestination)) {
                    selectedDestination = square;
                    selectedDestinationPane = cellMap.get(square);
                    commitSelectedMove();
                } else {
                    showInfoMessage("Ξεκίνα από το πορτοκαλί τετράγωνο "
                            + (primaryDestination != null ? primaryDestination.getId() : ""));
                }
                return;
            }
        }
    }

    private void handlePawnSelection(Pawn pawn) {
        if (!isMyTurn()) {
            return;
        }
        if (lastDice == null) {
            return;
        }
        if (!pawn.getOwner().equals(myPlayer)) {
            showErrorMessage("Το πιόνι του " + pawn.getOwner().getName() + " δεν μπορεί να μετακινηθεί από εσένα.");
            return;
        }
        if (!client.getGameEngine().canMove(pawn, lastDice)) {
            showErrorMessage("Δεν μπορεί να μετακινηθεί με " + lastDice);
            return;
        }

        if (selectedPawn != null && selectedPawn.equals(pawn)) {
            clearSelectionVisuals();
            return;
        }

        clearSelectionVisuals();
        clearMessage();

        showInfoMessage("Επέλεξες το πιόνι " + pawn.getOwner().getName() + " #" + pawn.getId());
        System.out.println("Pawn selected: " + pawn.getOwner().getName() + " #" + pawn.getId());

        selectedPawn = pawn;
        BoardSquare source = pawn.getPosition();
        selectedSourcePane = source != null ? cellMap.get(source) : null;
        if (selectedSourcePane != null) {

            selectedSourcePane.setStyle(SELECTED_PAWN_STYLE);
        }

        List<BoardSquare> available = client.getGameEngine().simulateDestinations(pawn, lastDice);
        if (available.isEmpty()) {
            showErrorMessage("Δεν υπάρχουν διαθέσιμα τετράγωνα για το πιόνι αυτό.");
            clearSelectionVisuals();
            return;
        }

        highlightedDestinations.clear();
        primaryDestination = available.get(0);
        finalDestination = available.size() > 1 ? available.get(available.size() - 1) : primaryDestination;

        StringBuilder sb = new StringBuilder("Διαθέσιμα τετράγωνα:");
        for (int i = 0; i < available.size(); i++) {
            BoardSquare dest = available.get(i);
            if (dest == null) {
                sb.append(" HOME");
                continue;
            }
            if (dest instanceof HomeSquare) {
                sb.append(" HOME");
            } else {
                sb.append(' ').append(dest.getId());
            }
            Pane pane = cellMap.get(dest);
            if (pane != null) {
                boolean highlightAsIntermediate = available.size() > 1 && i == 0;
                pane.setStyle(highlightAsIntermediate ? INTERMEDIATE_STYLE : DESTINATION_STYLE);
            }
            if (dest != null) {
                highlightedDestinations.add(dest);
            }
        }

        if (primaryDestination == null) {
            highlightedDestinations.clear();
        }

        if (!highlightedDestinations.isEmpty()) {
            finalDestination = highlightedDestinations.get(highlightedDestinations.size() - 1);
        } else {
            finalDestination = primaryDestination;
        }

        String infoMessage;
        if (available.size() > 1 && primaryDestination != null && finalDestination != null
                && !primaryDestination.equals(finalDestination)) {
            infoMessage = "Πορτοκαλί: " + primaryDestination.getId() + " → Κόκκινο: " + finalDestination.getId();
        } else {
            infoMessage = sb.toString().trim();
        }
        showInfoMessage(infoMessage);

        if (available.size() == 1) {
            selectedDestination = available.get(0);
            selectedDestinationPane = selectedDestination != null ? cellMap.get(selectedDestination) : null;
        } else {
            selectedDestination = null;
            selectedDestinationPane = null;
        }
    }

    private void commitSelectedMove() {
        if (selectedPawn == null || selectedDestination == null || lastDice == null) {
            clearSelectionVisuals();
            return;
        }
        boolean spawnMove = selectedPawn.getPosition() == selectedPawn.getBaseSquare();
        BoardSquare destinationToSend = finalDestination != null ? finalDestination : selectedDestination;
        if (destinationToSend == null) {
            clearSelectionVisuals();
            return;
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("MOVE ")
                .append(myPlayer.getId())
                .append(' ')
                .append(selectedPawn.getId())
                .append(' ')
                .append(destinationToSend.getId());
        if (spawnMove) {
            cmd.append(" SPAWN");
        }
        client.send(cmd.toString());
        clearHighlights();
        lastDice = null;
        rollButton.setDisable(true);
        passButton.setDisable(true);
        clearMessage();
    }

    private void clearSelectionVisuals() {
        if (selectedSourcePane != null && selectedPawn != null) {
            BoardSquare pos = selectedPawn.getPosition();
            restorePaneStyle(selectedSourcePane, pos);
        }
        if (selectedDestinationPane != null) {
            restorePaneStyle(selectedDestinationPane, selectedDestination);
        }
        if (!highlightedDestinations.isEmpty()) {
            for (BoardSquare dest : highlightedDestinations) {
                Pane pane = cellMap.get(dest);
                restorePaneStyle(pane, dest);
            }
            highlightedDestinations.clear();
        }
        clearSelectionState();
    }

    private void clearSelectionState() {
        selectedPawn = null;
        selectedDestination = null;
        primaryDestination = null;
        finalDestination = null;
        selectedSourcePane = null;
        selectedDestinationPane = null;
    }

    private void restorePaneStyle(Pane pane, BoardSquare square) {
        if (pane == null)
            return;
        if (square != null && validMoveSquares.contains(square)) {
            pane.setStyle(VALID_MOVE_STYLE);
        } else {
            pane.setStyle(DEFAULT_BORDER_STYLE);
        }
    }


    private StackPane pawnNode(Pawn pawn, double radius) {
        Circle c = pawnCircle(pawn, radius);
        StackPane sp = new StackPane(c);
        sp.setCursor(Cursor.HAND);
        sp.setOnMouseClicked(evt -> {
            evt.consume();
            BoardSquare pawnSquare = pawn.getPosition();

            if (selectedPawn != null && highlightedDestinations.contains(pawnSquare)) {
                if (primaryDestination != null && !pawnSquare.equals(primaryDestination)
                        && highlightedDestinations.size() > 1) {
                    showInfoMessage("Ξεκίνα από το πορτοκαλί τετράγωνο " + primaryDestination.getId());
                    return;
                }
                selectedDestination = pawnSquare;
                selectedDestinationPane = cellMap.get(pawnSquare);
                commitSelectedMove();
                return;
            }

            if (selectedPawn != null && selectedDestination != null && selectedDestination.equals(pawnSquare)
                    && !pawn.getOwner().equals(myPlayer)) {
                commitSelectedMove();
                return;
            }

            handlePawnSelection(pawn);
        });
        return sp;
    }

}
