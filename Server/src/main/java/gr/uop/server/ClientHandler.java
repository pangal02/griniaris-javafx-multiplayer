package gr.uop.server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;

    private volatile int id;
    private volatile String name = "Unknown";
    private volatile String color = "NONE";
    private volatile boolean ready = false;
    private volatile int extraRollsAvailable = 0;
    private volatile boolean sixBonusGranted = false;
    private volatile int lastRollValue = 0;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            // ignore
        } finally {
            server.removeClient(this);
            close();
        }
    }

    private void handleCommand(String msg) throws IOException {
        if (msg.startsWith("JOIN")) {
            // JOIN <name>
            String[] parts = msg.split(" ", 2);
            server.handleJoin(this, parts[1]);
        }
        else if(msg.startsWith("START")) {
            server.handleStart(this);
        }
        else if (msg.startsWith("COLOR")) {
            // COLOR <id> <color>
            String[] parts = msg.split(" ");
            if (parts.length >= 3) {
                server.handleColor(this, parts[2].toUpperCase());
            }
        }
        else if (msg.startsWith("READY")) {
            // READY <id> <true/false>
            String[] parts = msg.split(" ");
            if (parts.length >= 3) {
                boolean r = Boolean.parseBoolean(parts[2]);
                server.handleReady(this, r);
            }
        }
        else if (msg.startsWith("ROLL")) {
            // ROLL <id> <value>
            String[] parts = msg.split(" ");
            if (parts.length >= 3) {
                int value = Integer.parseInt(parts[2]);
                server.handleRoll(this, value);
            }
        }
        else if (msg.startsWith("MOVE")) {
            // MOVE <id> <pawnId> <toId> [SPAWN]
            String[] parts = msg.split(" ");
            if (parts.length >= 4) {
                int pawnId = Integer.parseInt(parts[2]);
                int toId = Integer.parseInt(parts[3]);
                boolean spawn = parts.length >= 5 && "SPAWN".equalsIgnoreCase(parts[4]);
                server.handleMove(this, pawnId, toId, spawn);
            }
        }
        else if (msg.startsWith("PASS")) {
            // PASS <id>
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                server.handlePass(this);
            }
        }
        else {
            System.out.println("📩 Άγνωστη εντολή: " + msg);
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    // getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public int getExtraRollsAvailable() { return extraRollsAvailable; }
    public void setExtraRollsAvailable(int value) { this.extraRollsAvailable = Math.max(0, value); }
    public void incrementExtraRolls() { extraRollsAvailable++; }
    public void decrementExtraRolls() { if (extraRollsAvailable > 0) extraRollsAvailable--; }

    public boolean isSixBonusGranted() { return sixBonusGranted; }
    public void setSixBonusGranted(boolean value) { this.sixBonusGranted = value; }

    public int getLastRollValue() { return lastRollValue; }
    public void setLastRollValue(int lastRollValue) { this.lastRollValue = lastRollValue; }

    public void resetTurnState() {
        extraRollsAvailable = 0;
        sixBonusGranted = false;
        lastRollValue = 0;
    }
}
