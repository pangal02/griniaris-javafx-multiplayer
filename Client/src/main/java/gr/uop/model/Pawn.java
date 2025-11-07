package gr.uop.model;

import javafx.scene.paint.Color;

public class Pawn {
    private final int id;
    private final Color color;        // Χρώμα παίκτη
    private final Player owner;       // Παίκτης
    private BoardSquare position; // Πού βρίσκεται (ή null αν δεν έχει βγει)
    private BoardSquare baseSquare; // Το τετράγωνο εκκίνησης
    private boolean isAtHome;   // Αν έχει τερματίσει

    public Pawn(int id, Color color, Player owner) {
        this.id = id;
        this.color = color;
        this.owner = owner;
        this.position = null; // Αρχικά δεν έχει βγει
        this.baseSquare = null; // Θα οριστεί όταν τοποθετηθεί στο ταμπλό
        this.isAtHome = false; // αν τερματισε
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public BoardSquare getPosition() {
        return position;
    }

    public BoardSquare getBaseSquare() {
        return baseSquare;
    }
    public void setBaseSquare(BoardSquare baseSquare) {
        this.baseSquare = baseSquare;
    }

    public void setPosition(BoardSquare position) {
        this.position = position;
    }

    public Player getOwner() {
        return owner;
    }

    public boolean isAtHome() {
        return isAtHome;
    }

    public void setAtHome(boolean atHome) {
        isAtHome = atHome;
    }

    public boolean isInBase() {
        return position.getId() == baseSquare.getId();
    }

    public String getColorString() {
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(Color.LIME) || color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        return "UNKNOWN";
    }

    @Override
    public String toString() {
        if (isAtHome) return "Pawn(" + getColorString() + ", HOME)";
        if (position == null) return "Pawn(" + getColorString() + ", BASE)";
        return "Pawn(" + getColorString() + ", at " + position.getId() + ")";
    }
}
