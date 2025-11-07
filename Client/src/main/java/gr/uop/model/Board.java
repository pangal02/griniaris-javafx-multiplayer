package gr.uop.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;

public class Board {
    private Map<Color, List<BoardSquare>> baseCells;
    private List<BoardSquare> mainPath; // 52 κελια
    private Map<Color, List<BoardSquare>> finals; // τελικά ανά χρώμα
    private Map<Color, BoardSquare> startSquares; // αρχικό τετράγωνο ανά χρώμα
    private Map<Color, List<BoardSquare>> home; // τετράγωνα στο σπίτι ανά χρώμα

    public Board() {
        mainPath = new ArrayList<>();
        finals = new HashMap<>();
        startSquares = new HashMap<>();
        home = new HashMap<>();
        baseCells = new HashMap<>();
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < 52; i++) {
            // Base squares ανά χρώμα
            if (i == 0) {
                mainPath.add(new BaseSquare(i, Color.RED));
                continue;
            }
            else if (i == 13) {
                mainPath.add(new BaseSquare(i, Color.BLUE));
                continue;
            }
            else if (i == 26) {
                mainPath.add(new BaseSquare(i, Color.YELLOW));
                continue;
            }
            else if (i == 39) {
                mainPath.add(new BaseSquare(i, Color.LIME));
                continue;
            }

            // Ειδικά τετράγωνα STAR / SHIELD
            else if (i == 3 || i == 16 || i == 29 || i == 42) {
                mainPath.add(new StarSquare(i));
            } 
            else if (i == 7 || i == 20 || i == 33 || i == 46) {
                mainPath.add(new ShieldSquare(i));
            } 
            else {
                mainPath.add(new BoardSquare(i, Color.TRANSPARENT));
            }
        }
        baseCells.put(Color.RED, List.of(new BaseSquare(-100, Color.RED), new BaseSquare(-101, Color.RED), new BaseSquare(-102, Color.RED), new BaseSquare(-103, Color.RED)));
        baseCells.put(Color.BLUE, List.of(new BaseSquare(-200, Color.BLUE), new BaseSquare(-201, Color.BLUE), new BaseSquare(-202, Color.BLUE), new BaseSquare(-203, Color.BLUE)));
        baseCells.put(Color.YELLOW, List.of(new BaseSquare(-300, Color.YELLOW), new BaseSquare(-301, Color.YELLOW), new BaseSquare(-302, Color.YELLOW), new BaseSquare(-303, Color.YELLOW)));
        baseCells.put(Color.LIME, List.of(new BaseSquare(-400, Color.LIME), new BaseSquare(-401, Color.LIME), new BaseSquare(-402, Color.LIME), new BaseSquare(-403, Color.LIME)));

        // --- Αρχικά τετράγωνα για κάθε χρώμα ---
        startSquares.put(Color.RED, mainPath.get(0));
        startSquares.put(Color.BLUE, mainPath.get(13));
        startSquares.put(Color.YELLOW, mainPath.get(26));
        startSquares.put(Color.LIME, mainPath.get(39));

        // --- Τελικά τετράγωνα για κάθε χρώμα ---
        finals.put(Color.RED, createFinalRow(Color.RED, 100));
        finals.put(Color.BLUE, createFinalRow(Color.BLUE, 200));
        finals.put(Color.YELLOW, createFinalRow(Color.YELLOW, 300));
        finals.put(Color.LIME, createFinalRow(Color.LIME, 400));

        // --- Τετράγωνα στο σπίτι για κάθε χρώμα ---
        home.put(Color.RED, List.of(new HomeSquare(105, Color.RED)));
        home.put(Color.BLUE, List.of(new HomeSquare(205, Color.BLUE)));
        home.put(Color.YELLOW, List.of(new HomeSquare(305, Color.YELLOW)));
        home.put(Color.LIME, List.of(new HomeSquare(405, Color.LIME)));
    }

    public List<BoardSquare> getFinalRowFor(Color color) {
        return finals.get(color);
    }

    public List<BoardSquare> getBaseCellsFor(Color color) {
        return baseCells.get(color);
    }

    private List<BoardSquare> createFinalRow(Color color, int baseId) {
        List<BoardSquare> row = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            row.add(new FinalSquare(baseId + i, color));
        }
        return row;
    }

    public List<BoardSquare> getMainPath() {
        return mainPath;
    }

    // Επιστρέφει το start square του χρώματος
    public BoardSquare getStartSquareFor(Color color) {
        return startSquares.get(color);
    }

    public List<BoardSquare> getHomeSquaresFor(Color color) {
        return home.get(color);
    }

    // Επιστρέφει το επόμενο square για το πιόνι (λαμβάνει υπόψη αν πάει σε finals)
    public BoardSquare getNextSquare(Pawn pawn, BoardSquare currentSquare, int steps) {
        int currentIndex = mainPath.indexOf(currentSquare);

        // Αν είναι ήδη στα finals → απλά πήγαινε μπροστά
        if (currentSquare instanceof FinalSquare) {
            List<BoardSquare> finalRow = finals.get(pawn.getColor());
            int index = finalRow.indexOf(currentSquare);
            int newIndex = index + steps;
            if (newIndex < finalRow.size()) {
                return finalRow.get(newIndex);
            } else if (newIndex == finalRow.size()) {

                // ΠΡΟΣΟΧΗ ΔΕΝ ΞΕΡΩ ΑΝ ΕΙΝΑΙ ΣΩΣΤΟ ΑΥΤΟ
                return home.get(pawn.getColor()).get(0); // Πήγαινε στο home
            } else {
                return currentSquare; // Μείνε στο τρέχον τετράγωνο
            }
        }

        // Αν δεν είναι στα finals → κινείται στην main path
        int newIndex = (currentIndex + steps) % mainPath.size();

        // Αν φτάνει στο σημείο εισόδου στα finals
        if (entersFinal(pawn, currentIndex, steps)) {
            List<BoardSquare> finalRow = finals.get(pawn.getColor());
            return finalRow.get(0);
        }
        return mainPath.get(newIndex);
    }

    private boolean entersFinal(Pawn pawn, int currentIndex, int steps) {
        int finalEntryIndex = getFinalEntryIndex(pawn.getColor());
        int targetIndex = (currentIndex + steps) % mainPath.size();
        return (targetIndex == finalEntryIndex);
    }

    private int getFinalEntryIndex(Color color) {
        if (color.equals(Color.RED))
            return 51;
        if (color.equals(Color.BLUE))
            return 12;
        if (color.equals(Color.YELLOW))
            return 25;
        if (color.equals(Color.LIME))
            return 38;
        return -1;
    }

    public int distanceToHome(Pawn pawn, BoardSquare currentSquare) {
        if (currentSquare instanceof FinalSquare) {
            List<BoardSquare> finalRow = finals.get(pawn.getColor());
            int index = finalRow.indexOf(currentSquare);
            return (finalRow.size() - index) + 1; // +1 για home
        }
        // Από main path → απόσταση μέχρι την είσοδο finals + 4 + 1
        int currentIndex = mainPath.indexOf(currentSquare);
        int entryIndex = getFinalEntryIndex(pawn.getColor());
        int distance = (entryIndex - currentIndex + mainPath.size()) % mainPath.size();
        return distance + finals.get(pawn.getColor()).size() + 1;
    }

    public BoardSquare getNextStarFor(BoardSquare currentSquare) {
        int index = mainPath.indexOf(currentSquare);
        int pathSize = mainPath.size();

        // clockwise scan for next star
        for (int i = 1; i < pathSize; i++) {
            int nextIndex = (index + i) % pathSize; // wraps around
            BoardSquare sq = mainPath.get(nextIndex);
            if (sq instanceof StarSquare) {
                System.out.println("[GET NEXT STAR] current=" + currentSquare.getId() + " next=" + sq.getId());

                // dont return a star that would wrap back to starting star (circle)
                if (nextIndex != index) {
                    return sq;
                }
            }
        }
        return null; // no valid next star
    }


    public void sendToBase(Pawn pawn) {
        // Αφαίρεση από την τρέχουσα θέση και επιστροφή στη βάση (position = base square)
        if (pawn.getPosition() != pawn.getBaseSquare() && pawn.getPosition() != null) {
            pawn.getPosition().removePawn(pawn);
        }
        pawn.setPosition(pawn.getBaseSquare());
    }

    public BoardSquare getSquareById(int squareId) {
        // Έλεγχος στα main path
        for (BoardSquare square : mainPath) {
            if (square.getId() == squareId) {
                return square;
            }
        }
        // Έλεγχος στα finals
        for (List<BoardSquare> finalRow : finals.values()) {
            for (BoardSquare square : finalRow) {
                if (square.getId() == squareId) {
                    return square;
                }
            }
        }
        // Έλεγχος στις βάσεις
        for (List<BoardSquare> baseRow : baseCells.values()) {
            for (BoardSquare square : baseRow) {
                if (square.getId() == squareId) {
                    return square;
                }
            }
        }
        // Έλεγχος στα home
        for (List<BoardSquare> homeSquares : home.values()) {
            for (BoardSquare square : homeSquares) {
                if (square.getId() == squareId) {
                    return square;
                }
            }
        }
        return null; // Αν δεν βρεθεί το τετράγωνο
    }
}
