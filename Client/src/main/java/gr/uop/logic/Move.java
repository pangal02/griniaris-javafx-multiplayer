package gr.uop.logic;

import gr.uop.model.*;

public class Move {
    private final Pawn pawn;
    private final BoardSquare fromBoardSquare;
    private final BoardSquare toBoardSquare;
    private final boolean ateOpponent;
    private final boolean formedBlock;
    private final boolean usedStar;
    private final boolean reachedHome;

    public Move(Pawn pawn, BoardSquare fromBoardSquare, BoardSquare toBoardSquare,
                boolean ateOpponent, boolean formedBlock,
                boolean usedStar, boolean reachedHome) {
        this.pawn = pawn;
        this.fromBoardSquare = fromBoardSquare;
        this.toBoardSquare = toBoardSquare;
        this.ateOpponent = ateOpponent;
        this.formedBlock = formedBlock;
        this.usedStar = usedStar;
        this.reachedHome = reachedHome;
    }

    // Getters
    public Pawn getPawn() { return pawn; }
    public BoardSquare getFromBoardSquare() { return fromBoardSquare; }
    public BoardSquare getToBoardSquare() { return toBoardSquare; }
    public boolean ateOpponent() { return ateOpponent; }
    public boolean formedBlock() { return formedBlock; }
    public boolean usedStar() { return usedStar; }
    public boolean reachedHome() { return reachedHome; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(pawn.getOwner().getName())
          .append(" (").append(pawn.getColorString()).append(") ");

        sb.append("moved from ");
        sb.append(fromBoardSquare != null ? fromBoardSquare.getId() : "START");
        sb.append(" to ");
        sb.append(toBoardSquare != null ? toBoardSquare.getId() : "HOME");

        if (ateOpponent) sb.append(" [ate opponent]");
        if (formedBlock) sb.append(" [formed block]");
        if (usedStar) sb.append(" [used star]");
        if (reachedHome) sb.append(" [reached home]");

        return sb.toString();
    }
}

