package gr.uop.model;

import javafx.scene.paint.Color;

public class HomeSquare extends BoardSquare {
    public HomeSquare(int id, Color color) {
        super(id, color);
    }

    @Override
    public boolean canEnter(Pawn pawn) {
        // Μόνο το ίδιο χρώμα μπορεί να μπει στο σπίτι του
        return pawn.getColor().equals(this.getColor());
    }

    @Override
    public BoardSquare postEnterRedirect(Board board, Pawn pawn) {
        // Ολοκλήρωση πιονιού
        addPawn(pawn);
        pawn.setPosition(this);
        pawn.setAtHome(true);
        return null;
    }
}
