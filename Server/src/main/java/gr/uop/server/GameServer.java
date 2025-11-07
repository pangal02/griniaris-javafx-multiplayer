package gr.uop.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Ο κεντρικός διακομιστής του παιχνιδιού.
 * Δέχεται συνδέσεις και διαχειρίζεται πολλούς clients ταυτόχρονα.
 */
public class GameServer {

    private final int port;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Consumer<String> logger;
    private final Set<String> usedColors = ConcurrentHashMap.newKeySet();
    private final Set<Integer> usedIds = ConcurrentHashMap.newKeySet();
    private int currentPlayerIndex = -1;
    private boolean gameStarted = false;
    private volatile ClientHandler host = null;

    private ServerSocket serverSocket;
    private boolean running = false;

    public GameServer(int port, Consumer<String> logger) {
        this.port = port;
        this.logger = logger;
    }

    public void start() {
        running = true;
        threadPool.execute(this::acceptClients);
    }

    private void acceptClients() {
        try {
            serverSocket = new ServerSocket(port);
            logger.accept("💡 Server listening on port " + port);

            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                threadPool.execute(handler);
                logger.accept("👤 Νέος παίκτης συνδέθηκε από: " + socket.getInetAddress());
            }
        } catch (IOException e) {
            if (running)
                logger.accept("❌ Σφάλμα στον server: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    private synchronized void startGame() {
        logger.accept("START");
        broadcast("");
    }

    // Επεξεργασία JOIN
    public synchronized void handleJoin(ClientHandler client, String name) {
        client.setName(name);
        client.setId(generateId());
        client.setColor("NONE");
        client.send("ID " + client.getId());
        if (host == null) {
            host = client;
        }
        broadcastPlayerList();
        logger.accept("👤 " + name + " συνδέθηκε (id=" + client.getId() + ")");
    }

    // Επεξεργασία START
    public synchronized void handleStart(ClientHandler requester) {
        if (host == null || requester != host) {
            if (requester != null) {
                requester.send("START_DENIED HOST_ONLY");
            }
            logger.accept("⚠️ Ο παίκτης " + (requester != null ? requester.getName() : "unknown") + " προσπάθησε να ξεκινήσει χωρίς να είναι host.");
            return;
        }
        if (gameStarted) {
            logger.accept("⚠️ Το παιχνίδι έχει ήδη ξεκινήσει.");
            return;
        }
        if (clients.size() < 2) {
            logger.accept("⚠️ Απαιτούνται τουλάχιστον 2 παίκτες για να ξεκινήσει το παιχνίδι.");
            return;
        }
        gameStarted = true;
        broadcast("START");
        logger.accept("🚀 Το παιχνίδι ξεκίνησε από τον διακομιστή!");
        //threadPool.execute(this::nextTurn);
        nextTurn();
    }

    // Επεξεργασία COLOR
    public synchronized void handleColor(ClientHandler client, String newColor) {
        if (usedColors.contains(newColor) && !newColor.equals(client.getColor())) {
            client.send("COLOR_DENIED " + newColor);
            logger.accept("⚠️ Το χρώμα " + newColor + " είναι ήδη δεσμευμένο.");
            return;
        }
        // Ελευθέρωσε το παλιό χρώμα (αν υπήρχε)
        if (!"NONE".equals(client.getColor())) {
            usedColors.remove(client.getColor());
        }
        client.setColor(newColor);
        usedColors.add(newColor);
        broadcast("COLOR " + client.getId() + " " + newColor);
        logger.accept("🎨 Ο παίκτης " + client.getName() + " επέλεξε " + newColor + ".");
        broadcastPlayerList();
    }

    // Επεξεργασία READY
    public synchronized void handleReady(ClientHandler client, boolean ready) {

        client.setReady(ready);
        broadcast("READY " + client.getId() + " " + (ready ? "TRUE" : "FALSE"));
        logger.accept("📣 Ο παίκτης " + client.getName() + " δήλωσε " + (ready ? "έτοιμος" : "μη έτοιμος") + ".");
        broadcastPlayerList();
        checkAllReady();
    }

    private void checkAllReady() {
        boolean allReady = !clients.isEmpty() && clients.stream().allMatch(ClientHandler::isReady)
                && clients.size() >= 2;
        if (allReady) {
            broadcast("ALL_READY");
            logger.accept("✅ Όλοι οι παίκτες είναι έτοιμοι! Μπορείτε να ξεκινήσετε το παιχνίδι.");
        }
    }

    // Επεξεργασία ROLL
    public synchronized void handleRoll(ClientHandler client, int value) {
        if (clients.isEmpty() || client != clients.get(currentPlayerIndex)) {
            logger.accept("⚠️ Δεν είναι η σειρά του παίκτη " + client.getName() + " να ρίξει τα ζάρια.");
            return;
        }

        client.setLastRollValue(value);
        if (value == 6 && !client.isSixBonusGranted()) {
            client.incrementExtraRolls();
            client.setSixBonusGranted(true);
        }

        broadcast("ROLL " + client.getId() + " " + value);
        logger.accept("🎲 Τα " + client.getColor() + " έφεραν " + value + ".");
    }

    public synchronized void handleMove(ClientHandler client, int pawnId, int toId, boolean spawnMove) {
        if (clients.isEmpty() || client != clients.get(currentPlayerIndex)) {
            logger.accept("⚠️ Δεν είναι η σειρά του παίκτη " + client.getName() + " να κινηθεί.");
            return;
        }

        broadcast("MOVE " + client.getId() + " " + pawnId + " " + toId);
        logger.accept("🚶 Τα " + client.getColor() + " μετέφεραν πιόνι " + pawnId + " στη θέση " + toId + ".");

        if (spawnMove) {
            client.incrementExtraRolls();
        }

        if (client.getExtraRollsAvailable() > 0) {
            client.decrementExtraRolls();
            broadcast("TURN " + client.getId());
            logger.accept("🔁 Ο παίκτης " + client.getName() + " έχει επιπλέον ρίψη.");
        } else {
            nextTurn();
        }
    }

    public synchronized void handlePass(ClientHandler client) {
        if (clients.isEmpty() || client != clients.get(currentPlayerIndex)) {
            logger.accept("⚠️ Δεν είναι η σειρά του παίκτη " + client.getName() + " να περάσει.");
            return;
        }

        broadcast("PASS " + client.getId());
        logger.accept("⏭️ Ο παίκτης " + client.getName() + " πέρασε τη σειρά του.");
        client.resetTurnState();
        nextTurn();
    }

    // ==============================================================

    public synchronized void broadcast(String message) {
        // Snapshot για να αποφύγουμε το send σε κλειστά sockets
        List<ClientHandler> snapshot = new ArrayList<>(clients);
        for (ClientHandler c : snapshot) {
            try {
                c.send(message);
            } catch (Exception e) {
                // Αγνόησε αν το socket έκλεισε
                logger.accept("⚠️ Αποτυχία αποστολής σε " + c.getName());
            }
        }
    }

    public synchronized void broadcastPlayerList() {
        List<ClientHandler> snapshot = new ArrayList<>(clients);
        for (ClientHandler c : snapshot) {
            for (ClientHandler p : snapshot) {
                try {
                    c.send("PLAYER " + p.getId() + " " + p.getName() + " " + p.getColor() + " " + p.isReady());
                } catch (Exception e) {
                    logger.accept("⚠️ Αποτυχία αποστολής λίστας σε " + c.getName());
                }
            }
        }
        broadcastHost();
    }
    // ==============================================================

    public synchronized void nextTurn() {
        if (clients.isEmpty())
            return;
        currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
        ClientHandler currentPlayer = clients.get(currentPlayerIndex);
        currentPlayer.resetTurnState();
        broadcast("TURN " + currentPlayer.getId());
        logger.accept("🔄 Είναι η σειρά του " + currentPlayer.getName() + " (" + currentPlayer.getColor() + ").");
    }

    public synchronized void removeClient(ClientHandler client) {
        int removedIndex = clients.indexOf(client);
        if (removedIndex < 0) {
            return; // Ο client δεν βρέθηκε στη λίστα
        }
        
        clients.remove(client);
        logger.accept("👋 Παίκτης " + client.getName() + " αποσυνδέθηκε.");
        boolean hostChanged = (client == host);
        if (hostChanged) {
            host = clients.isEmpty() ? null : clients.get(0);
        }
        
        if (clients.size() < 2 && gameStarted) {
            broadcast("GAME_ABORTED");
            logger.accept("⚠️ Το παιχνίδι ακυρώθηκε λόγω αποσύνδεσης παίκτη.");
            gameStarted = false;
            currentPlayerIndex = -1;
            for (ClientHandler c : clients) {
                c.setReady(false);
            }
            broadcastPlayerList();
            return;
        }
        
        broadcast("PLAYER_LEFT " + client.getId());
        broadcastPlayerList();
        
        // Αν το παιχνίδι δεν έχει ξεκινήσει, δεν χρειάζεται να διαχειριστούμε σειρές
        if (!gameStarted) {
            return;
        }
        
        // Διαχείριση currentPlayerIndex μετά την αφαίρεση
        if (clients.isEmpty()) {
            currentPlayerIndex = -1;
            return;
        }
        
        // Αν αφαιρέθηκε παίκτης πριν από τον τρέχοντα, μείωσε το index
        if (removedIndex < currentPlayerIndex) {
            currentPlayerIndex--;
        }
        // Αν αφαιρέθηκε ο τρέχων παίκτης (removedIndex == currentPlayerIndex)
        // το index μένει το ίδιο, δείχνοντας στον επόμενο
        else if (removedIndex == currentPlayerIndex) {
            // Wrap around αν χρειάζεται
            if (currentPlayerIndex >= clients.size()) {
                currentPlayerIndex = 0;
            }
            // Στείλε TURN για τον νέο τρέχοντα παίκτη
            ClientHandler newCurrent = clients.get(currentPlayerIndex);
            broadcast("TURN " + newCurrent.getId());
            logger.accept("🔄 Είναι η σειρά του " + newCurrent.getName() + " (" + newCurrent.getColor() + ").");
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (!clients.isEmpty()) {
                broadcast("SERVER_SHUTDOWN");
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler c : clients) {
                c.close();
            }
            clients.clear();
            host = null;
            threadPool.shutdownNow();
            logger.accept("🛑 Server σταμάτησε επιτυχώς.");
        } catch (IOException e) {
            logger.accept("⚠️ Σφάλμα στο κλείσιμο: " + e.getMessage());
        }
    }

    private synchronized int generateId() {
        Random random = new Random();
        int id;
        int attempts = 0;
        do {
            id = random.nextInt(9000) + 1000;
            attempts++;
            if (attempts > 10000) {
                throw new IllegalStateException("Δεν μπόρεσε να δημιουργήσει μοναδικό ID");
            }
        } while (usedIds.contains(id));
        usedIds.add(id);
        return id;
    }

    private void broadcastHost() {
        broadcast("HOST " + (host != null ? host.getId() : -1));
    }
}
