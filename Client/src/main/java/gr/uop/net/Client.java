package gr.uop.net;

import gr.uop.logic.GameEngine;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final GameEngine engine;
    private int playerId;
    private final String name;
    private Color playerColor;
    private volatile Consumer<String> messageHandler; // Μεταβλητός handler

    public Client(String ip, int port, String name) throws IOException {
        this.socket = new Socket(ip, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.engine = new GameEngine();
        this.name = name;

        // Ξεκίνα το listener thread μία φορά με ΣΤΑΘΕΡΟ callback
        startListenerThread();
        
        // ❌ ΑΦΑΙΡΕΘΗΚΕ - Το JOIN στέλνεται από το LobbyScene μετά το setup του callback
        // send("JOIN " + name);
    }

    /** Ξεκινάει το listener thread που θα τρέχει μόνο μία φορά */
    private void startListenerThread() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String msg = line.trim();
                    System.out.println("🔌 Client received: " + msg); // DEBUG
                    
                    // Κάλεσε τον ΤΡΕΧΟΝΤΑ handler (μπορεί να αλλάζει)
                    Consumer<String> handler = messageHandler;
                    if (handler != null) {
                        handler.accept(msg);
                    }
                }
            } catch (IOException e) {
                System.err.println("Σφάλμα σύνδεσης: " + e.getMessage());
            }
        }).start();
    }

    /** Στέλνει μήνυμα στον server */
    public void send(String msg) {
        out.println(msg);
    }

    /** Αλλάζει τον handler για τα μηνύματα (thread-safe με volatile) */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    /** @deprecated Use setMessageHandler instead */
    public void setMessageCallback(Consumer<String> callback) {
        setMessageHandler(callback);
    }
    
    /** @deprecated Use setMessageHandler instead */
    public void listen(Consumer<String> callback) {
        setMessageHandler(callback);
    }

    public GameEngine getGameEngine() {
        return engine;
    }

    public void setPlayerId(int id){
        this.playerId = id;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(Color playerColor) {
        this.playerColor = playerColor;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedReader getBufferedReader() {
        return in;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }
}
