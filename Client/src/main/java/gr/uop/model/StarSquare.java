package gr.uop.model;

public class StarSquare extends BoardSquare {
    public StarSquare(int id) {
        super(id, null);
    }

    @Override
    public BoardSquare postEnterRedirect(Board board, Pawn pawn) {
        // place pawn on this star
        addPawn(pawn);
        pawn.setPosition(this);

        // compute next star. If it wraps around the main path (completes a round), do not redirect.
        BoardSquare nextStar = board.getNextStarFor(this);
        int curIndex = board.getMainPath().indexOf(this);
        int nextIndex = board.getMainPath().indexOf(nextStar);

        
        // debug 
        System.out.println("[STAR] pawn=" + pawn.getId() + " color=" + pawn.getColor()
            + " onStar=" + this.getId() + " nextStar=" + (nextStar != null ? nextStar.getId() : "null")
            + " curIndex=" + curIndex + " nextIndex=" + nextIndex);
        if (nextIndex >= 0 && curIndex >= 0 && nextIndex < curIndex) {
            return null; // wrapping -> completes a round: stay
        }
        return nextStar;
    }
}

