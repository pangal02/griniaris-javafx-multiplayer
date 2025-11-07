package gr.uop.model;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

public class Player {
    private int id;
    private String name;
    private Color color;
    private List<Pawn> pawns;
    private boolean isReady;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.pawns = new ArrayList<>();
        this.isReady = false;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
    public void initializePlayersPawns() {
        for(int i = 0; i < 4; i++){
            pawns.add(new Pawn( i, color, this));
        }
    }

    public String getName() {
        return name;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    public Color getColor() {
        return color;
    }

    public List<Pawn> getPawns() {
        return pawns;
    }

    public void addPawn(Pawn pawn) {
        pawns.add(pawn);
    }

    public boolean isAllAtHome() {
        for (Pawn p : pawns){
            if(!p.isAtHome()){
                return false;
            }
        }
        return true;
    }

    public List<Pawn> getPawnsAtBoard(){
        List<Pawn> pawnsAtBoard = new ArrayList<>();
        for (Pawn p : pawns){
            if(p.getPosition() != null){
                pawnsAtBoard.add(p);
            }
        }
        return pawnsAtBoard;
    }

    public int pawnsInBase() {
        int count = 0;
        for (Pawn p : pawns) {
            if (p.isInBase()) count++;
        }
        return count;
    }

    // Επιστρέφει αν ο παίκτης μπορεί να βγάλει πιόνι (π.χ. έφερε 5)
    public boolean canSpawnPawn(int diceValue) {
        return (diceValue == 5 && pawnsInBase() > 0);
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }
    public Pawn getPawnById(int pawnId) {
        for (Pawn pawn : pawns) {
            if (pawn.getId() == pawnId) {
                return pawn;
            }
        }
        return null; // Αν δεν βρεθεί πιόνι με το συγκεκριμένο ID
    }

    @Override
    public String toString() {
        String colorStr;
        if (this.color == Color.RED) {
            colorStr = "Red";
        } else if (this.color == Color.BLUE) {
            colorStr = "Blue";
        } else if (this.color == Color.GREEN || this.color == Color.LIME) {
            colorStr = "Green";
        } else if (this.color == Color.YELLOW) {
            colorStr = "Yellow";
        } else {
            colorStr = "Unknown Color";
        }
        return id + ". " + this.name  + " " + colorStr + " " + (isReady ? "(Έτοιμος)" : "(Όχι έτοιμος)");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player other = (Player) obj;
        return id == other.id;
    }
}
