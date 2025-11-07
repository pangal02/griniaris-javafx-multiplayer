package gr.uop.model;

import javafx.scene.paint.Color;

public class FinalSquare extends BoardSquare {
    public FinalSquare(int id, Color ownerColor) {
        super(id, ownerColor);
    }

    @Override
    public boolean canEnter(Pawn pawn) {
        // Μόνο το αντίστοιχο χρώμα επιτρέπεται
        if (getColor() == null || !getColor().equals(pawn.getColor())) return false;
        return super.canEnter(pawn);
    }
}
