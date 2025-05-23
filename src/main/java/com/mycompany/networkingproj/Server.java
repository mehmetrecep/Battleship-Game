/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.networkingproj;

/**
 *
 * @author moustafa
 */
import java.nio.channels.ServerSocketChannel;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private ServerSocketChannel serverSocket;
    private Selector selector;
    private final CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Player> waitingPlayers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port), 50);
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + port);

            Thread matchmakingThread = new Thread(() -> {
                while (true) {
                    try {
                        matchPlayers();
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        System.err.println("Error in matchmaking: " + e.getMessage());
                    }
                }
            });
            matchmakingThread.setDaemon(true);
            matchmakingThread.start();

            while (true) {
                int readyChannels = selector.selectNow();
                if (readyChannels > 0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (!key.isValid()) continue;

                        if (key.isAcceptable()) {
                            acceptClient();
                        } else if (key.isReadable()) {
                            readClientMessage(key);
                        } else if (key.isWritable()) {
                            writeClientMessage(key);
                        }
                    }
                }

                for (Player player : new ArrayList<>(players)) {
                    if (!player.isConnected()) {
                        handlePlayerDisconnect(player);
                    }
                }

                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    private void matchPlayers() {
        synchronized (waitingPlayers) {
            if (waitingPlayers.size() >= 2) {
                System.out.println("Matchmaking: " + waitingPlayers.size() + " players waiting");
                List<Player> shuffled = new ArrayList<>(waitingPlayers);
                Collections.shuffle(shuffled, random);

                for (int i = 0; i < shuffled.size() - 1; i += 2) {
                    Player player1 = shuffled.get(i);
                    Player player2 = shuffled.get(i + 1);

                    if (player1.isConnected() && player2.isConnected()) {
                        createGame(player1, player2);
                        waitingPlayers.remove(player1);
                        waitingPlayers.remove(player2);
                    }

                    if (waitingPlayers.size() < 2) break;
                }
            }
        }
    }

    private void createGame(Player player1, Player player2) {
        String gameId = System.currentTimeMillis() + "-" + random.nextInt(1000);
        Game game = new Game(gameId, player1, player2);
        activeGames.put(gameId, game);

        player1.setCurrentGameId(gameId);
        player2.setCurrentGameId(gameId);

        game.start();
        System.out.println("Game " + gameId + " started with players: " + player1.getName() + " vs " + player2.getName());
    }

    private void acceptClient() throws IOException {
        System.out.println("Attempting to accept new client...");
        SocketChannel client = serverSocket.accept();
        if (client == null) {
            System.out.println("No client to accept");
            return;
        }
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        Player player = new Player(client);
        players.add(player);
        player.sendMessage("WELCOME TO BATTLESHIP");
        player.sendMessage("ENTER_NAME");

        System.out.println("New client connected, IP: " + client.getRemoteAddress() + ", total players: " + players.size());
    }

    private void readClientMessage(SelectionKey key) throws IOException {
        Player player = findPlayerByChannel((SocketChannel) key.channel());
        if (player == null) {
            System.out.println("No player found for channel");
            return;
        }

        try {
            String message = player.readMessage();
            if (message == null) {
                handlePlayerDisconnect(player);
                return;
            }

            if (!message.isEmpty()) {
                processClientMessage(player, message);
            }
        } catch (IOException e) {
            System.out.println("Error reading from " + (player.getName() != null ? player.getName() : "unnamed") + ": " + e.getMessage());
            handlePlayerDisconnect(player);
        }
    }

    private void writeClientMessage(SelectionKey key) throws IOException {
        Player player = findPlayerByChannel((SocketChannel) key.channel());
        if (player == null) {
            System.out.println("No player found for channel");
            return;
        }

        try {
            player.flushMessages();
        } catch (IOException e) {
            System.out.println("Error writing to " + (player.getName() != null ? player.getName() : "unnamed") + ": " + e.getMessage());
            handlePlayerDisconnect(player);
        }
    }

    private void processClientMessage(Player player, String message) {
        System.out.println("Received from " + (player.getName() != null ? player.getName() : "unnamed") + ": " + message);

        if (message.equals("QUIT")) {
            handlePlayerDisconnect(player);
        } else if (player.getName() == null) {
            player.setName(message);
            player.sendMessage("HELLO|" + message);

            synchronized (waitingPlayers) {
                waitingPlayers.add(player);
            }

            player.sendMessage("WAITING_FOR_OPPONENT|" + waitingPlayers.size());
            broadcastWaitingCount();
        } else if (message.equals("FIND_GAME")) {
            if (!waitingPlayers.contains(player)) {
                synchronized (waitingPlayers) {
                    waitingPlayers.add(player);
                }
                player.sendMessage("WAITING_FOR_OPPONENT|" + waitingPlayers.size());
                broadcastWaitingCount();
            }
        } else if (message.startsWith("PLACE_SHIPS")) {
            Game game = getPlayerGame(player);
            if (game != null) {
                game.handleShipPlacement(player, message);
            }
        } else if (message.startsWith("FIRE")) {
            Game game = getPlayerGame(player);
            if (game != null && game.isPlayerTurn(player)) {
                game.handleShot(player, message);
            }
        }
    }

    private Game getPlayerGame(Player player) {
        if (player.getCurrentGameId() != null) {
            return activeGames.get(player.getCurrentGameId());
        }
        return null;
    }

    private void broadcastWaitingCount() {
        synchronized (waitingPlayers) {
            for (Player p : waitingPlayers) {
                p.sendMessage("WAITING_COUNT|" + waitingPlayers.size());
            }
        }
    }

    private void handlePlayerDisconnect(Player player) {
        synchronized (waitingPlayers) {
            waitingPlayers.remove(player);
        }

        if (player.getCurrentGameId() != null) {
            Game game = activeGames.get(player.getCurrentGameId());
            if (game != null) {
                game.handleDisconnect(player);

                if (!game.hasActivePlayers()) {
                    activeGames.remove(player.getCurrentGameId());
                    System.out.println("Game " + player.getCurrentGameId() + " removed due to all players disconnected");
                }
            }
        }

        player.disconnect();
        players.remove(player);
        System.out.println("Player " + (player.getName() != null ? player.getName() : "unnamed") +
                " disconnected, total players: " + players.size() +
                ", waiting: " + waitingPlayers.size());

        broadcastWaitingCount();
    }

    private void stop() {
        try {
            if (selector != null) selector.close();
            if (serverSocket != null) serverSocket.close();
            for (Player player : players) {
                player.disconnect();
            }
            System.out.println("Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    private Player findPlayerByChannel(SocketChannel channel) {
        for (Player player : players) {
            if (player.getChannel() == channel) {
                return player;
            }
        }
        return null;
    }

    
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {   
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default 8080");
            }
        }
        Server server = new Server(port);
        server.start();
    }
}