package gr.uop.model;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

public class BoardSquare {
    private final int id; // μοναδικό αναγνωριστικό
    private final Color color; // για START/FINAL, σε ποιον παίκτη ανήκει
    private final List<Pawn> pawns; // πιόνια που βρίσκονται εδώ

    public BoardSquare(int id, Color color) {
        this.id = id;
        this.color = color != null ? color : Color.TRANSPARENT;
        this.pawns = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public List<Pawn> getPawns() {
        return pawns;
    }

    public void addPawn(Pawn p) {
        pawns.add(p);
    }

    public void removePawn(Pawn p) {
        pawns.remove(p);
    }

    public boolean isOccupied() {
        return !pawns.isEmpty();
    }

    // Επιστρέφει αν το τετράγωνο είναι "φράγμα" για άλλους
    public boolean isBlockedFor(Pawn pawn) {
        // A square forms a block if it has >=2 pawns of the same color.
        // Other players cannot pass through or stop there; owner can.
        if (pawns.size() < 2) return false;
        Color blockColor = pawns.get(0).getColor();
        boolean uniform = pawns.stream().allMatch(x -> x.getColor().equals(blockColor));
        if (!uniform) return false;
        return !blockColor.equals(pawn.getColor());
    }

    // ---- Game rules helpers (default behavior; ειδικές περιπτώσεις σε υποκλάσεις) ----

    // Επιτρέπει το πέρασμα (για υπολογισμό διαδρομής). Φράγμα άλλου χρώματος μπλοκάρει.
    public boolean canPassThrough(Pawn pawn) {
        return !isBlockedFor(pawn);
    }

    // Επιτρέπει να σταματήσει ο pawn εδώ σύμφωνα με τους βασικούς κανόνες κατάληψης.
    public boolean canEnter(Pawn pawn) {
        // Shield τετράγωνα: επιτρέπουν συνύπαρξη αντιπάλων
        if (this instanceof ShieldSquare) return true;

        if (pawns.isEmpty()) return true;

        boolean sameColor = pawns.stream().allMatch(x -> x.getColor().equals(pawn.getColor()));

        if (sameColor) {
            // Επιτρέπουμε έως 2 ίδια πιόνια (φράγμα). >2 δεν επιτρέπεται.
            return pawns.size() < 2;
        } else {
            // Διαφορετικού χρώματος: επιτρέπεται (θα γίνει capture) εκτός αν έχουν ήδη
            // δημιουργηθεί περισσότερα από 1 πιόνια (δεν θα πρέπει να συμβεί εκτός SHIELD)
            return pawns.size() == 1;
        }
    }

    // Καλείται όταν ο pawn πατήσει στο τετράγωνο. Default: capture αντιπάλου σε non-SHIELD.
    // Επιστρέφει redirect square (π.χ. για αστέρι), αλλιώς null.
    public BoardSquare postEnterRedirect(Board board, Pawn pawn) {
        if (!(this instanceof ShieldSquare)) {
            // απομάκρυνση αντιπάλων
            List<Pawn> toRemove = new ArrayList<>();
            for (Pawn p : pawns) {
                if (!p.getColor().equals(pawn.getColor())) {
                    toRemove.add(p);
                }
            }
            for (Pawn opp : toRemove) {
                pawns.remove(opp);
                board.sendToBase(opp);
            }
        }
        addPawn(pawn);
        pawn.setPosition(this);
        return null;
    }

    private String getColorString() {
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(Color.LIME)) return "GREEN";
        if (color.equals(Color.YELLOW)) return "YELLOW";
        return "NONE";
    }

    @Override
    public String toString() {
        return "BoardSquare{" +
                "id=" + id +
                ", color=" + getColorString() +
                ", pawns=" + pawns.size() +
                '}';
    }
}
