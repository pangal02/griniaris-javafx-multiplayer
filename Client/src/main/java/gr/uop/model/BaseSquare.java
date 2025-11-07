package gr.uop.model;

import javafx.scene.paint.Color;

public class BaseSquare extends BoardSquare {
    public BaseSquare(int id, Color color) {
        super(id, color);
    }

    @Override
    public BaseSquare postEnterRedirect(Board board, Pawn pawn) {
        // Μην κάνεις capture, απλά πρόσθεσε
        addPawn(pawn);
        pawn.setPosition(this);
        return null;
    }
    
}
