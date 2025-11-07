package gr.uop.logic;

import java.util.ArrayList;
import java.util.List;
import gr.uop.model.*;
import javafx.scene.paint.Color;

public class GameEngine {
    private Board board;
    private Dice dice;
    private List<Player> players;
    private int currentPlayerIndex;

    public GameEngine() {
        this.dice = new Dice();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.board = new Board();
    }

    public void initializeBoard() {
        for (Player player : players) {
            List<BoardSquare> base = board.getBaseCellsFor(player.getColor());
            for (int i = 0; i < player.getPawns().size(); i++) {
                Pawn pawn = player.getPawns().get(i);
                BoardSquare square = base.get(i);
                pawn.setPosition(square);
                pawn.setBaseSquare(square);
                square.addPawn(pawn);
            }
        }
        System.out.println("Η σκακιέρα αρχικοποιήθηκε με " + players.size() + " παίκτες.");
    }

    public Board getBoard() { return board; }
    public List<Player> getPlayers() { return players; }

    public List<Player> setPlayersList(List<Player> players) {
        if (players.size() > 4) {
            throw new IllegalStateException("Υπάρχουν περισσότεροι από 4 παίκτες.");
        } else if (players.size() < 2) {
            throw new IllegalStateException("Απαιτούνται τουλάχιστον 2 παίκτες.");
        }
        this.players = new ArrayList<>(players);
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).initializePlayersPawns();
        }
        initializeBoard();
        return this.players;
    }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public Dice getDice() { return dice; }
    public int rollDice() { 
        System.out.println("Ο παίκτης " + getCurrentPlayer().getName() + " ρίχνει το ζάρι...");
        dice.roll();
        System.out.println("Αποτέλεσμα ζαριού: " + dice.getValue());
        return dice.getValue();
    }
    public void nextTurn() { currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); }

    public List<Pawn> getValidMoves(int diceValue) {
        List<Pawn> valid = new ArrayList<>();
        for (Pawn p : getCurrentPlayer().getPawns()) {
            if (canMove(p, diceValue)) valid.add(p);
        }
        return valid;
    }

    public Pawn getPawnById(Player player, int pawnId) {
        for (Pawn pawn : player.getPawns()) {
            if (pawn.getId() == pawnId) return pawn;
        }
        return null;
    }

    public Player getPlayerByColor(Color color) {
        for (Player p : players) {
            if (p.getColor().equals(color)) return p;
        }
        return null;
    }

    public Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public boolean canMove(Pawn pawn, int diceValue) {
        if (pawn.isAtHome()) return false;

        BoardSquare from = pawn.getPosition();

        if (from == pawn.getBaseSquare()) {
            if (diceValue != 5) {
                System.out.println("cant move from home, dice value: " + diceValue);
                return false;
            }
            BoardSquare start = board.getStartSquareFor(pawn.getColor());
            return start.canEnter(pawn);
        }

        // Must not overshoot HOME
        int distToHome = distanceToHomeSimulated(pawn, from);
        if (diceValue > distToHome) return false;

        // Check pass-through blocking and final cell enterability
        BoardSquare cur = from;
        for (int step = 1; step <= diceValue; step++) {
            BoardSquare next = stepForward(pawn, cur);
            
            // stepForward returns null when reaching HOME (after FinalRow)
            if (next == null) {
                // HOME reached on this step - valid only if exact dice match
                return step == diceValue;
            }
            
            // Also check if next IS the HomeSquare object itself (redundant safety check)
            if (next == board.getHomeSquaresFor(getCurrentPlayer().getColor()).get(0)) {
                System.out.println("next == HOME " + "(" + next + ") (step " + step + ")");
                return step == diceValue;
            }
            
            // Check if we can pass through or enter this square
            if (step < diceValue) {
                if (!next.canPassThrough(pawn)) return false;
            } else {
                if (!next.canEnter(pawn)) return false;
            }
            cur = next;
        }
        return true;
    }

    public Move movePawn(Pawn pawn, int diceValue) {
        System.out.println("[ENTER MOVEPAWN] pawn=" + pawn.getId() + " dice=" + diceValue);

        BoardSquare from = pawn.getPosition();
        BoardSquare to = null;
        boolean ate = false;
        boolean block = false;
        boolean star = false;
        boolean home = false;

        // Spawn from base (BaseSquare)
        if (from == pawn.getBaseSquare()) {
            if (from instanceof BaseSquare) {
                from.removePawn(pawn);
            }
            to = board.getStartSquareFor(pawn.getColor());
            boolean hadOpponents = to.getPawns().stream().anyMatch(p -> p.getColor() != pawn.getColor());
            boolean shield = (to instanceof ShieldSquare);
            BoardSquare redirect = to.postEnterRedirect(board, pawn);
            if (hadOpponents && !shield) ate = true;
            if (redirect != null) {
                star = true;
                to.removePawn(pawn);
                to = redirect;
            }
            return new Move(pawn, from, to, ate, block, star, home);
        }

        // Walk step-by-step to final destination
        BoardSquare cur = from;
        for (int step = 1; step <= diceValue; step++) {
            BoardSquare next = stepForward(pawn, cur);
            if (next == null) { // HOME
                home = true;
                break;
            }
            cur = next;
        }

        if (home) {
            from.removePawn(pawn);
            
            // Προσθήκη του πιονιού στο HomeSquare για visual representation
            HomeSquare homeSquare = (HomeSquare) board.getHomeSquaresFor(pawn.getColor()).get(0);
            homeSquare.addPawn(pawn);
            pawn.setPosition(homeSquare);
            pawn.setAtHome(true);
            
            return new Move(pawn, from, homeSquare, ate, block, star, true);
        }

        to = cur;
        boolean hadOpponents = to.getPawns().stream().anyMatch(p -> p.getColor() != pawn.getColor());
        boolean shield = (to instanceof ShieldSquare);

        from.removePawn(pawn);
        System.out.println("[DEBUG] Entering square " + to.getId() + " of type " + to.getClass().getSimpleName());

        BoardSquare redirect = to.postEnterRedirect(board, pawn);
        if (hadOpponents && !shield) ate = true;

        if (redirect != null) {
            // One star jump
            star = true;
            to.removePawn(pawn);
            //redirect.postEnterRedirect(board, pawn);
            to = redirect;
        }

        if (to.getPawns().size() >= 2 && allSameColors(to.getPawns())) {
            block = true;
        }          
        System.out.println("[FINAL MOVE] pawn=" + pawn.getId() 
            + " final square=" + to.getId() 
            + " class=" + to.getClass().getSimpleName() 
            + " star=" + star + " home=" + home);
        return new Move(pawn, from, to, ate, block, star, home);

    }

    public void movePawnTo(Pawn pawn, BoardSquare destination) {
        BoardSquare from = pawn.getPosition();
        if (from != null) {
            from.removePawn(pawn);
        }
        pawn.setAtHome(false);
        if (destination != null) {
            destination.addPawn(pawn);
            pawn.setPosition(destination);
            if (destination instanceof HomeSquare) {
                pawn.setAtHome(true);
                if (hasWin(getCurrentPlayer())) {
                    System.out.println("Ο παίκτης " + getCurrentPlayer().getName() + " κέρδισε το παιχνίδι!");
                    return;
                }
            }
        } else {
            pawn.setPosition(null);
        }
    }

    public void setCurrentPlayerById(int playerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId() == playerId) {
                currentPlayerIndex = i;
                return;
            }
        }
        throw new IllegalArgumentException("Μη έγκυρος παίκτης με ID: " + playerId);
    }

    public void setCurrentPlayer(Player player) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).equals(player)) {
                currentPlayerIndex = i;
                return;
            }
        }
        throw new IllegalArgumentException("Ο παίκτης δεν βρέθηκε στη λίστα.");
    }

    public synchronized void applyRemoteMove(int playerId, int pawnId, int toSquareId) {
        Player player = getPlayerById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Μη έγκυρος παίκτης με ID: " + playerId);
        }

        Pawn pawn = getPawnById(player, pawnId);
        if (pawn == null) {
            throw new IllegalArgumentException("Μη έγκυρο πιόνι με ID: " + pawnId);
        }

        BoardSquare destination = board.getSquareById(toSquareId);
        if (destination == null) {
            throw new IllegalArgumentException("Μη έγκυρη πλατεία προορισμού με ID: " + toSquareId);
        }

        // Remove from old square (if any)
        BoardSquare from = pawn.getPosition();
        if (from != null) {
            from.removePawn(pawn);
        }
        pawn.setAtHome(false);

        // --- directly place pawn at the server-approved destination ---
        destination.addPawn(pawn);
        pawn.setPosition(destination);
        if (destination instanceof HomeSquare) {
            pawn.setAtHome(true);
            if (hasWin(getCurrentPlayer())) {
                System.out.println("Ο παίκτης " + getCurrentPlayer().getName() + " κέρδισε το παιχνίδι!");
                return;
            }
        }
    }



    private boolean allSameColors(List<Pawn> pawns) {
        return pawns.stream().map(Pawn::getColor).distinct().count() == 1;
    }

    public boolean hasWin(Player player) {
        return player.isAllAtHome();
    }

    public Player checkWinner() {
        for (Player p : players) if (p.isAllAtHome()) return p;
        return null;
    }

    
    // Helpers for path simulation
    private int getFinalEntryIndex(Color color) {
        if (color.equals(Color.RED)) return 50;
        if (color.equals(Color.BLUE)) return 11;
        if (color.equals(Color.YELLOW)) return 24;
        if (color.equals(Color.LIME)) return 37;
        return -1;
    }

    private BoardSquare stepForward(Pawn pawn, BoardSquare currentSquare) {
        if (currentSquare instanceof FinalSquare) {
            List<BoardSquare> finalRow = board.getFinalRowFor(pawn.getColor());
            int idx = finalRow.indexOf(currentSquare);
            if (idx < 0) return currentSquare;
            if (idx + 1 < finalRow.size()) return finalRow.get(idx + 1);
            return null; // HOME
        }
        List<BoardSquare> path = board.getMainPath();
        int curIdx = path.indexOf(currentSquare);
        if (curIdx < 0)
            return currentSquare;
        if (curIdx == getFinalEntryIndex(pawn.getColor())) {
            return board.getFinalRowFor(pawn.getColor()).get(0);
        }
        return path.get((curIdx + 1) % path.size());
    }

    private int distanceToHomeSimulated(Pawn pawn, BoardSquare from) {
        int dist = 0;
        BoardSquare cur = from;
        while (true) {
            BoardSquare next = stepForward(pawn, cur);
            dist++;
            if (next == null) return dist; // HOME reached on this step
            cur = next;
            if (dist > board.getMainPath().size() + board.getFinalRowFor(pawn.getColor()).size() + 2) {
                return dist;
            }
        }
    }

    // Simulation helper for UI preview: returns destination square after diceValue steps,
    // including a potential single star redirect. Returns null if HOME is reached.
    public BoardSquare simulateDestination(Pawn pawn, int diceValue) {
        BoardSquare from = pawn.getPosition();
        if (from == null) {
            // spawn to start on 5
            if (diceValue != 5) return null;
            return board.getStartSquareFor(pawn.getColor());
        }
        BoardSquare cur = from;
        for (int step = 1; step <= diceValue; step++) {
            BoardSquare next = stepForward(pawn, cur);
            if (next == null) return null; // home
            cur = next;
        }
        // star redirect preview (no wrap)
        if (cur instanceof StarSquare) {
            BoardSquare nextStar = board.getNextStarFor(cur);
            int curIdx = board.getMainPath().indexOf(cur);
            int nextIdx = board.getMainPath().indexOf(nextStar);

            System.out.println("[STAR DEBUG] pawn=" + pawn.getId() +
                " landedOn=" + cur.getId() +
                " nextStar=" + (nextStar != null ? nextStar.getId() : "null") +
                " curIdx=" + curIdx + " nextIdx=" + nextIdx);

            // If nextStar exists and is NOT the same as current → jump
            if (nextStar != null && nextStar != cur) {
                return nextStar;
            }
        }
        return cur;
    }

    public List<BoardSquare> simulateDestinations(Pawn pawn, int diceValue) {
        List<BoardSquare> result = new ArrayList<>();
        if (diceValue <= 0 || pawn.isAtHome()) {
            return result;
        }

        // spawn from base
        if (pawn.getPosition() == pawn.getBaseSquare()) {
            if (diceValue == 5) {
                BoardSquare start = board.getStartSquareFor(pawn.getColor());
                if (start != null && start.canEnter(pawn)) {
                    result.add(start);
                }
            }
            return result;
        }

        BoardSquare current = pawn.getPosition();
        BoardSquare landing = null;

        for (int step = 1; step <= diceValue; step++) {
            BoardSquare next = stepForward(pawn, current);
            if (next == null) {
                // Φτάνουμε στο HOME - προσθέτουμε το HomeSquare αντί για null
                if (step == diceValue) {
                    HomeSquare homeSquare = (HomeSquare) board.getHomeSquaresFor(pawn.getColor()).get(0);
                    result.add(homeSquare);
                }
                return result;
            }

            if (step < diceValue) {
                if (!next.canPassThrough(pawn)) {
                    return result;
                }
                current = next;
                continue;
            }

            if (!next.canEnter(pawn)) {
                return result;
            }
            landing = next;
        }

        if (landing != null) {
            result.add(landing);
            if (landing instanceof StarSquare) {
                BoardSquare nextStar = board.getNextStarFor(landing);
                int currentIdx = board.getMainPath().indexOf(landing);
                int nextIdx = board.getMainPath().indexOf(nextStar);

                System.out.println("[SIMULATE DEST] pawn=" + pawn.getId() +
                    " landedOn=" + landing.getId() +
                    " nextStar=" + (nextStar != null ? nextStar.getId() : "null") +
                    " currentIdx=" + currentIdx + " nextIdx=" + nextIdx);

                // ACTUAL JUMP
                if (nextStar != null && nextStar != landing && nextStar.canEnter(pawn)) {
                    result.add(nextStar);
                }
            }
        }

        return result;
    }
}
