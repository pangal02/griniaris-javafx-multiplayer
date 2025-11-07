package gr.uop.model;

public class ShieldSquare extends BoardSquare {
    public ShieldSquare(int id) {
        super(id, null);
    }

    @Override
    public boolean canEnter(Pawn pawn) {
        // Επιτρέπεται συνύπαρξη αντιπάλων
        return true;
    }

    @Override
    public BoardSquare postEnterRedirect(Board board, Pawn pawn) {
        // Μην κάνεις capture, απλά πρόσθεσε
        addPawn(pawn);
        pawn.setPosition(this);
        return null;
    }
}
