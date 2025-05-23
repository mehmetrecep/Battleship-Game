/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.networkingproj;

/**
 *
 * @author moustafa
 */

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Client extends JFrame {
    private final String hostname;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private JTextField inputField;
    private JButton connectButton;
    private JButton findGameButton;
    private JLabel statusLabel;
    private JLabel waitingPlayersLabel;
    private JTextArea logArea;
    private JButton[][] playerGrid = new JButton[10][10];
    private JButton[][] opponentGrid = new JButton[10][10];
    private boolean placingShips = false;
    private boolean myTurn = false;
    private List<int[]> shipPlacements = new ArrayList<>(); 
    private int currentShipIndex = 0;
    private final int[] shipLengths = {5, 4, 3, 3, 2}; 
    private final String[] shipNames = {"Carrier (5)", "Battleship (4)", "Cruiser (3)", "Submarine (3)", "Destroyer (2)"};
    private boolean horizontal = true;
    private boolean[][] tempShips = new boolean[10][10]; // Track placed ships for overlap checking
    private JLabel shipStatusLabel; 
    private JLabel orientationLabel; 

    private final Color[] shipColors = {
            new Color(70, 130, 180),  
            new Color(255, 69, 0),    
            new Color(50, 205, 50),   
            new Color(186, 85, 211),  
            new Color(255, 215, 0)    
    };

    private int[][] shipGrid = new int[10][10]; // 0 = no ship, 1-5 = ship index+1

    private List<GameAction> gameActions = new ArrayList<>();
    private int currentReplayIndex = 0;
    private boolean inReplayMode = false;
    private JButton replayButton;
    private JButton nextActionButton;
    private JButton prevActionButton;
    private JButton exitReplayButton;
    private JPanel replayControlPanel;
    private Timer replayTimer;
    private JLabel replayStatusLabel;

    private boolean[][] initialPlayerShips = new boolean[10][10];
    private boolean[][] initialOpponentShips = new boolean[10][10]; // This would be empty/unknown
    private JComboBox<String> replaySpeedComboBox;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Battleship Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        inputField = new JTextField(20);
        connectButton = new JButton("Connect");
        findGameButton = new JButton("Find Game");
        findGameButton.setEnabled(false);
        statusLabel = new JLabel("Disconnected");
        waitingPlayersLabel = new JLabel("Waiting: 0");

        topPanel.add(new JLabel("Input:"));
        topPanel.add(inputField);
        topPanel.add(connectButton);
        topPanel.add(findGameButton);
        topPanel.add(statusLabel);
        topPanel.add(waitingPlayersLabel);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        JPanel playerPanel = new JPanel(new BorderLayout());
        JPanel opponentPanel = new JPanel(new BorderLayout());

        shipStatusLabel = new JLabel("Not placing ships");
        orientationLabel = new JLabel("Orientation: Horizontal");

        JPanel playerGridPanel = createLabeledGrid(playerGrid, true);
        JPanel opponentGridPanel = createLabeledGrid(opponentGrid, false);

        JPanel controlPanel = new JPanel(new GridLayout(3, 1));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton rotateButton = new JButton("Rotate Ship (R)");
        rotateButton.addActionListener(e -> {
            if (placingShips) {
                horizontal = !horizontal;
                updateOrientationLabel();
                logMessage("Orientation: " + (horizontal ? "Horizontal" : "Vertical"));
            }
        });

        JButton resetButton = new JButton("Reset Placement");
        resetButton.addActionListener(e -> {
            if (placingShips) {
                resetShipPlacement();
            }
        });

        replayButton = new JButton("Replay Game");
        replayButton.setEnabled(false);
        replayButton.addActionListener(e -> startReplay());

        buttonPanel.add(rotateButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(replayButton);

        JPanel legendPanel = new JPanel(new GridLayout(1, 5));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Ship Types (Placement Order)"));

        for (int i = 0; i < shipNames.length; i++) {
            JPanel shipPanel = new JPanel(new BorderLayout());
            JLabel colorSample = new JLabel();
            colorSample.setOpaque(true);
            colorSample.setBackground(shipColors[i]);
            colorSample.setPreferredSize(new Dimension(20, 20));
            shipPanel.add(colorSample, BorderLayout.WEST);
            shipPanel.add(new JLabel(" " + (i + 1) + ". " + shipNames[i]), BorderLayout.CENTER);
            legendPanel.add(shipPanel);
        }

        controlPanel.add(buttonPanel);
        controlPanel.add(shipStatusLabel);
        controlPanel.add(orientationLabel);

        playerPanel.add(playerGridPanel, BorderLayout.CENTER);
        playerPanel.add(controlPanel, BorderLayout.SOUTH);

        opponentPanel.add(opponentGridPanel, BorderLayout.CENTER);

        createReplayControlPanel();

        centerPanel.add(createTitledPanel("Your Grid", playerPanel));
        centerPanel.add(createTitledPanel("Opponent Grid", opponentPanel));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(legendPanel, BorderLayout.NORTH);

        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        bottomPanel.add(replayControlPanel, BorderLayout.SOUTH);
        replayControlPanel.setVisible(false);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> {
            if (connected) {
                disconnect();
            } else {
                connect();
            }
        });

        findGameButton.addActionListener(e -> {
            if (connected) {
                sendMessage("FIND_GAME");
                statusLabel.setText("Finding a game...");
                logMessage("Looking for an opponent...");
            }
        });

        inputField.addActionListener(e -> {
            if (connected) {
                sendMessage(inputField.getText());
                inputField.setText("");
            }
        });

        // Global key listener for rotation
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_R && placingShips) {
                horizontal = !horizontal;
                updateOrientationLabel();
                logMessage("Orientation: " + (horizontal ? "Horizontal" : "Vertical"));
                return true;
            }
            return false;
        });

        setFocusable(true);
        requestFocusInWindow();
        pack();
        setLocationRelativeTo(null);
    }

    private void createReplayControlPanel() {
        replayControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        prevActionButton = new JButton("⏪ Previous");
        nextActionButton = new JButton("Next ⏩");
        exitReplayButton = new JButton("Exit Replay");
        replayStatusLabel = new JLabel("Replay: 0/0");

        String[] speeds = {"0.5x Speed", "1x Speed", "2x Speed", "4x Speed"};
        replaySpeedComboBox = new JComboBox<>(speeds);
        replaySpeedComboBox.setSelectedIndex(1); // Default to 1x

        JButton autoPlayButton = new JButton("▶ Auto Play");
        JButton pauseButton = new JButton("⏸ Pause");
        pauseButton.setEnabled(false);

        autoPlayButton.addActionListener(e -> {
            autoPlayButton.setEnabled(false);
            pauseButton.setEnabled(true);
            startAutoReplay();
        });

        pauseButton.addActionListener(e -> {
            pauseButton.setEnabled(false);
            autoPlayButton.setEnabled(true);
            stopAutoReplay();
        });

        prevActionButton.addActionListener(e -> showPreviousAction());
        nextActionButton.addActionListener(e -> showNextAction());
        exitReplayButton.addActionListener(e -> exitReplay());

        replayControlPanel.add(replayStatusLabel);
        replayControlPanel.add(prevActionButton);
        replayControlPanel.add(autoPlayButton);
        replayControlPanel.add(pauseButton);
        replayControlPanel.add(nextActionButton);
        replayControlPanel.add(replaySpeedComboBox);
        replayControlPanel.add(exitReplayButton);
    }

    private void startAutoReplay() {
        if (replayTimer != null) {
            replayTimer.stop();
        }

        int delay;
        delay = switch (replaySpeedComboBox.getSelectedIndex()) {
            case 0 -> 2000;
            case 2 -> 500;
            case 3 -> 250;
            default -> 1000;
        }; 

        replayTimer = new Timer(delay, e -> {
            if (currentReplayIndex < gameActions.size() - 1) {
                showNextAction();
            } else {
                stopAutoReplay();
                for (Component c : replayControlPanel.getComponents()) {
                    if (c instanceof JButton && ((JButton) c).getText().equals("▶ Auto Play")) {
                        c.setEnabled(true);
                    }
                    if (c instanceof JButton && ((JButton) c).getText().equals("⏸ Pause")) {
                        c.setEnabled(false);
                    }
                }
            }
        });
        replayTimer.start();
    }

    private void stopAutoReplay() {
        if (replayTimer != null) {
            replayTimer.stop();
        }
    }

    private JPanel createLabeledGrid(JButton[][] grid, boolean isPlayerGrid) {
        JPanel gridPanel = new JPanel(new GridLayout(11, 11));

        gridPanel.add(new JLabel(""));
        for (int i = 0; i < 10; i++) {
            gridPanel.add(new JLabel(String.valueOf((char) ('A' + i)), SwingConstants.CENTER));
        }

        for (int i = 0; i < 10; i++) {
            gridPanel.add(new JLabel(String.valueOf(i + 1), SwingConstants.CENTER));
            for (int j = 0; j < 10; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(35, 35));
                button.setFocusable(false);
                final int row = i;
                final int col = j;

                if (isPlayerGrid) {
                    button.addActionListener(e -> handlePlayerGridClick(row, col));

                    button.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            if (placingShips && currentShipIndex < shipLengths.length) {
                                previewShipPlacement(row, col);
                            }
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            if (placingShips && currentShipIndex < shipLengths.length) {
                                clearPreview();
                            }
                        }
                    });
                } else {
                    button.addActionListener(e -> handleOpponentGridClick(row, col));
                }

                grid[i][j] = button;
                gridPanel.add(button);
            }
        }

        return gridPanel;
    }

    private void updateShipStatusLabel() {
        if (placingShips && currentShipIndex < shipLengths.length) {
            shipStatusLabel.setText("Placing: " + shipNames[currentShipIndex] +
                    " (" + (currentShipIndex + 1) + "/" + shipLengths.length + ")");
        } else if (placingShips) {
            shipStatusLabel.setText("All ships placed! Waiting for server confirmation...");
        } else {
            shipStatusLabel.setText("Not placing ships");
        }
    }

    private void updateOrientationLabel() {
        orientationLabel.setText("Orientation: " + (horizontal ? "Horizontal" : "Vertical"));
    }

    private void previewShipPlacement(int row, int col) {
        if (!placingShips || currentShipIndex >= shipLengths.length) return;

        int length = shipLengths[currentShipIndex];
        boolean valid = true;

        for (int j = 0; j < length; j++) {
            int r = row + (horizontal ? 0 : j);
            int c = col + (horizontal ? j : 0);
            if (r >= 10 || c >= 10 || tempShips[r][c]) {
                valid = false;
                break;
            }
        }

        Color previewColor = valid ?
                new Color(shipColors[currentShipIndex].getRed(),
                        shipColors[currentShipIndex].getGreen(),
                        shipColors[currentShipIndex].getBlue(), 150) : 
                new Color(255, 100, 100, 180); 

        for (int j = 0; j < length; j++) {
            int r = row + (horizontal ? 0 : j);
            int c = col + (horizontal ? j : 0);
            if (r < 10 && c < 10) {
                if (!tempShips[r][c]) {
                    playerGrid[r][c].setBackground(previewColor);
                }
            }
        }
    }

    private void clearPreview() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (!tempShips[i][j]) {
                    playerGrid[i][j].setBackground(UIManager.getColor("Button.background"));
                } else {
                    int shipType = shipGrid[i][j] - 1;
                    if (shipType >= 0 && shipType < shipColors.length) {
                        playerGrid[i][j].setBackground(shipColors[shipType]);
                    } else {
                        playerGrid[i][j].setBackground(Color.GRAY);
                    }
                }
            }
        }
    }

    private JPanel createTitledPanel(String title, JPanel panel) {
        JPanel titledPanel = new JPanel(new BorderLayout());
        titledPanel.setBorder(BorderFactory.createTitledBorder(title));
        titledPanel.add(panel, BorderLayout.CENTER);
        return titledPanel;
    }

    private void connect() {
        try {
            socket = new Socket(hostname, port);
            socket.setSoTimeout(100);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            connectButton.setText("Disconnect");
            statusLabel.setText("Connected to server");
            inputField.setEnabled(true);
            findGameButton.setEnabled(true);

            logMessage("Connected to " + hostname + ":" + port);
            listenForMessages();
        } catch (IOException e) {
            e.printStackTrace();
            logMessage("Connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server at " + hostname + ":" + port,
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("QUIT");
                out.flush();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            connected = false;
            connectButton.setText("Connect");
            statusLabel.setText("Disconnected");
            inputField.setEnabled(false);
            findGameButton.setEnabled(false);
            placingShips = false;
            resetGameState();
            logMessage("Disconnected from server");
        } catch (IOException e) {
            logMessage("Error disconnecting: " + e.getMessage());
        }
    }

    private void resetGameState() {
        currentShipIndex = 0;
        shipPlacements.clear();
        tempShips = new boolean[10][10];
        shipGrid = new int[10][10];
        SwingUtilities.invokeLater(this::resetPlayerGrid);
        updateShipStatusLabel();
        updateOrientationLabel();
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
            logMessage("Sent: " + message);
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                String message;
                while (connected && socket != null && !socket.isClosed()) {
                    try {
                        message = in.readLine();
                        if (message == null) {
                            SwingUtilities.invokeLater(this::disconnect);
                            break;
                        }
                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> processServerMessage(finalMessage));
                    } catch (SocketTimeoutException e) {
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
                SwingUtilities.invokeLater(this::disconnect);
            }
        }).start();
    }

    private void processServerMessage(String message) {
        String[] parts = message.split("\\|");
        switch (parts[0]) {
            case "WELCOME TO BATTLESHIP":
                logMessage("Server: Welcome to Battleship!");
                break;
            case "ENTER_NAME":
                logMessage("Enter your name:");
                inputField.requestFocus();
                break;
            case "HELLO":
                logMessage("Server: Hello, " + (parts.length > 1 ? parts[1] : "Player") + "!");
                break;
            case "WAITING_FOR_OPPONENT":
                logMessage("Waiting for opponent...");
                if (parts.length > 1) {
                    int count = Integer.parseInt(parts[1]);
                    waitingPlayersLabel.setText("Waiting: " + count);
                }
                break;
            case "WAITING_COUNT":
                if (parts.length > 1) {
                    int count = Integer.parseInt(parts[1]);
                    waitingPlayersLabel.setText("Waiting: " + count);
                }
                break;
            case "GAME_STARTING":
                logMessage("Game starting against " + (parts.length > 1 ? parts[1] : "opponent"));
                gameActions.clear();
                currentReplayIndex = 0;
                findGameButton.setEnabled(false);
                break;
            case "PLACE_YOUR_SHIPS":
                logMessage("Place your ships on the grid (press R to rotate)");
                logMessage("PLACEMENT INSTRUCTIONS: Click on a cell to place the starting point of your ship.");
                logMessage("Ships are placed one at a time in order: Carrier (5), Battleship (4), etc.");
                placingShips = true;
                resetGameState();
                SwingUtilities.invokeLater(() -> {
                    enablePlayerGrid();
                    updateShipStatusLabel();
                    requestFocusInWindow();
                });
                break;
            case "SHIPS_PLACED":
                logMessage("Ships placed successfully");
                updateShipStatusLabel();
                break;
            case "SHIP_POSITIONS":
                SwingUtilities.invokeLater(() -> {
                    displayFinalShips(Arrays.copyOfRange(parts, 1, parts.length));
                    // Save initial ship positions for replay
                    saveInitialShipPositions();
                });
                break;
            case "INVALID_PLACEMENT":
                logMessage("Invalid placement: " + (parts.length > 1 ? parts[1] : ""));
                if (!shipPlacements.isEmpty()) {
                    removeLastShipPlacement();
                    currentShipIndex--;
                    updateShipStatusLabel();
                    SwingUtilities.invokeLater(this::resetPlayerGrid);
                }
                break;
            case "BATTLE_STARTING":
                logMessage("Battle starting!");
                placingShips = false;
                updateShipStatusLabel();
                SwingUtilities.invokeLater(() -> {
                    disablePlayerGrid();
                    enableOpponentGrid();
                });
                break;
            case "YOUR_TURN":
                logMessage("Your turn!");
                myTurn = true;
                enableOpponentGrid();
                break;
            case "OPPONENT_TURN":
                logMessage("Opponent's turn...");
                myTurn = false;
                disableOpponentGrid();
                break;
            case "SHOT_RESULT":
                if (parts.length < 3) {
                    logMessage("Received invalid SHOT_RESULT format");
                    break;
                }
                try {
                    String[] coordinates = parts[1].split(",");
                    int row = Integer.parseInt(coordinates[0].trim());
                    int col = Integer.parseInt(coordinates[1].trim());
                    String result = parts[2];
                    gameActions.add(new GameAction(GameActionType.PLAYER_SHOT, row, col, result));
                    SwingUtilities.invokeLater(() -> updateOpponentGrid(row, col, result));
                    logMessage("Shot at (" + (char) ('A' + col) + (row + 1) + "): " + result);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    logMessage("Error parsing coordinates in SHOT_RESULT");
                }
                break;
            case "OPPONENT_SHOT":
                if (parts.length < 3) {
                    logMessage("Received invalid OPPONENT_SHOT format");
                    break;
                }
                try {
                    String[] coordinates = parts[1].split(",");
                    int row = Integer.parseInt(coordinates[0].trim());
                    int col = Integer.parseInt(coordinates[1].trim());
                    String result = parts[2];
                    // Record opponent's shot for replay
                    gameActions.add(new GameAction(GameActionType.OPPONENT_SHOT, row, col, result));
                    SwingUtilities.invokeLater(() -> updatePlayerGrid(row, col, result));
                    logMessage("Opponent shot at (" + (char) ('A' + col) + (row + 1) + "): " + result);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    logMessage("Error parsing coordinates in OPPONENT_SHOT");
                }
                break;
            case "VICTORY":
                logMessage("You win!");
                JOptionPane.showMessageDialog(this, "Victory!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                disableOpponentGrid();
                gameActions.add(new GameAction(GameActionType.GAME_OVER, "VICTORY"));
                replayButton.setEnabled(true);
                findGameButton.setEnabled(true);
                break;
            case "DEFEAT":
                logMessage("You lose!");
                JOptionPane.showMessageDialog(this, "Defeat!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                disableOpponentGrid();
                gameActions.add(new GameAction(GameActionType.GAME_OVER, "DEFEAT"));
                replayButton.setEnabled(true);
                findGameButton.setEnabled(true);
                break;
            case "OPPONENT_DISCONNECTED":
                logMessage("Opponent disconnected");
                JOptionPane.showMessageDialog(this, "Opponent disconnected", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                gameActions.add(new GameAction(GameActionType.OPPONENT_DISCONNECTED));
                replayButton.setEnabled(true);
                findGameButton.setEnabled(true);
                break;
            case "GAME_OVER":
                logMessage("Game over. " + (parts.length > 1 ? parts[1] : ""));
                findGameButton.setEnabled(true);
                break;
            default:
                logMessage("Server: " + message);
        }
    }

    private void saveInitialShipPositions() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                initialPlayerShips[i][j] = tempShips[i][j];
            }
        }
    }

    private void handlePlayerGridClick(int row, int col) {
        if (!placingShips || currentShipIndex >= shipLengths.length) {
            return;
        }

        int length = shipLengths[currentShipIndex];
        boolean valid = true;
        List<Point> cellsToHighlight = new ArrayList<>();

        for (int j = 0; j < length; j++) {
            int r = row + (horizontal ? 0 : j);
            int c = col + (horizontal ? j : 0);
            if (r >= 10 || c >= 10 || tempShips[r][c]) {
                valid = false;
                break;
            }
            cellsToHighlight.add(new Point(r, c));
        }

        if (valid) {
            Color shipColor = shipColors[currentShipIndex];
            SwingUtilities.invokeLater(() -> {
                for (Point p : cellsToHighlight) {
                    playerGrid[p.x][p.y].setBackground(shipColor);
                    playerGrid[p.x][p.y].repaint();

                    shipGrid[p.x][p.y] = currentShipIndex + 1;
                }
            });

            for (Point p : cellsToHighlight) {
                tempShips[p.x][p.y] = true;
            }

            if (!cellsToHighlight.isEmpty()) {
                Point startPoint = cellsToHighlight.get(0);
                playerGrid[startPoint.x][startPoint.y].setText(String.valueOf(currentShipIndex + 1));
            }

            shipPlacements.add(new int[]{row, col, horizontal ? 1 : 0});
            logMessage("Placed " + shipNames[currentShipIndex] +
                    " at " + (char) ('A' + col) + (row + 1) +
                    " (" + (horizontal ? "horizontal" : "vertical") + ")");

            currentShipIndex++;
            updateShipStatusLabel();

            if (currentShipIndex == shipLengths.length) {
                StringBuilder message = new StringBuilder("PLACE_SHIPS");
                for (int i = 0; i < shipPlacements.size(); i++) {
                    int[] placement = shipPlacements.get(i);
                    message.append("|").append(placement[0]).append(",").append(placement[1]).append(",")
                            .append(placement[2] == 1 ? "H" : "V");
                }
                sendMessage(message.toString());
            }
        } else {
            logMessage("Invalid placement: Out of bounds or overlapping with another ship");
        }
    }

    private void handleOpponentGridClick(int row, int col) {
        if (myTurn && !placingShips && !inReplayMode) {
            if (opponentGrid[row][col].getBackground() == Color.RED ||
                    opponentGrid[row][col].getBackground() == Color.BLUE) {
                logMessage("You've already fired at this position");
                return;
            }

            sendMessage("FIRE|" + row + "," + col);
            myTurn = false;
            disableOpponentGrid();
            logMessage("Firing at: " + (char) ('A' + col) + (row + 1));
        }
    }

    private void displayFinalShips(String[] positions) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (shipGrid[i][j] == 0) {
                        playerGrid[i][j].setBackground(UIManager.getColor("Button.background"));
                    } else {
                        int shipType = shipGrid[i][j] - 1;
                        if (shipType >= 0 && shipType < shipColors.length) {
                            playerGrid[i][j].setBackground(shipColors[shipType]);
                        } else {
                            playerGrid[i][j].setBackground(Color.GRAY);
                        }
                    }
                }
            }

            for (String pos : positions) {
                try {
                    String[] coords = pos.split(",");
                    int row = Integer.parseInt(coords[0].trim());
                    int col = Integer.parseInt(coords[1].trim());
                    playerGrid[row][col].setEnabled(false);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    logMessage("Error parsing ship positions");
                }
            }

            logMessage("All ships placed and confirmed by server");
        });
    }

    private void updatePlayerGrid(int row, int col, String result) {
        SwingUtilities.invokeLater(() -> {
            if (result.equals("HIT") || result.equals("SUNK")) {
                playerGrid[row][col].setBackground(new Color(255, 50, 50));
                playerGrid[row][col].setText("✗");
                playerGrid[row][col].setForeground(Color.WHITE);

                if (result.equals("SUNK")) {
                    logMessage("Your ship was sunk!");
                    markSunkShip(row, col, true);
                }
            } else {
                playerGrid[row][col].setBackground(new Color(100, 180, 255)); // Blue for miss
                playerGrid[row][col].setText("○");
                playerGrid[row][col].setForeground(Color.WHITE);
            }
            playerGrid[row][col].setEnabled(false);
        });
    }

    private void updateOpponentGrid(int row, int col, String result) {
        SwingUtilities.invokeLater(() -> {
            if (result.equals("HIT") || result.equals("SUNK")) {
                opponentGrid[row][col].setBackground(Color.RED);
                opponentGrid[row][col].setText("✗");
                opponentGrid[row][col].setForeground(Color.WHITE);

                if (result.equals("SUNK")) {
                    logMessage("You sunk an enemy ship!");
                    markSunkShip(row, col, false);
                }
            } else {
                opponentGrid[row][col].setBackground(Color.BLUE);
                opponentGrid[row][col].setText("○");
                opponentGrid[row][col].setForeground(Color.WHITE);
            }
            opponentGrid[row][col].setEnabled(myTurn);
        });
    }

    private void markSunkShip(int row, int col, boolean isPlayerGrid) {
        
        if (isPlayerGrid) {
            playerGrid[row][col].setForeground(Color.WHITE);
            playerGrid[row][col].setText("X");
        } else {
            opponentGrid[row][col].setForeground(Color.WHITE);
            opponentGrid[row][col].setText("X");
        }
    }

    private void resetPlayerGrid() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    playerGrid[i][j].setText("");
                    if (!tempShips[i][j]) {
                        playerGrid[i][j].setBackground(UIManager.getColor("Button.background"));
                    } else {
                        int shipType = shipGrid[i][j] - 1;
                        if (shipType >= 0 && shipType < shipColors.length) {
                            playerGrid[i][j].setBackground(shipColors[shipType]);
                            boolean isFirstCell = true;
                            if (i > 0 && shipGrid[i - 1][j] == shipGrid[i][j]) {
                                isFirstCell = false;
                            }
                            if (j > 0 && shipGrid[i][j - 1] == shipGrid[i][j]) {
                                isFirstCell = false;
                            }

                            if (isFirstCell) {
                                playerGrid[i][j].setText(String.valueOf(shipType + 1));
                            }
                        } else {
                            playerGrid[i][j].setBackground(Color.GRAY);
                            logMessage("Warning: Invalid ship type at position " + i + "," + j);
                        }
                    }
                }
            }
        });
    }

    private void resetShipPlacement() {
        shipPlacements.clear();
        currentShipIndex = 0;
        tempShips = new boolean[10][10];
        shipGrid = new int[10][10];
        updateShipStatusLabel();
        resetPlayerGrid();
        logMessage("Ship placement reset");
    }

    private void enablePlayerGrid() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    playerGrid[i][j].setEnabled(true);
                }
            }
            requestFocusInWindow();
        });
    }

    private void disablePlayerGrid() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    playerGrid[i][j].setEnabled(false);
                }
            }
        });
    }

    private void enableOpponentGrid() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (opponentGrid[i][j].getBackground() != Color.RED &&
                            opponentGrid[i][j].getBackground() != Color.BLUE) {
                        opponentGrid[i][j].setEnabled(true);
                    }
                }
            }
        });
    }

    private void disableOpponentGrid() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    opponentGrid[i][j].setEnabled(false);
                }
            }
        });
    }

    private void removeLastShipPlacement() {
        if (!shipPlacements.isEmpty()) {
            int[] lastPlacement = shipPlacements.get(shipPlacements.size() - 1);
            int row = lastPlacement[0];
            int col = lastPlacement[1];
            boolean horizontal = lastPlacement[2] == 1;
            int length = shipLengths[shipPlacements.size() - 1];

            for (int j = 0; j < length; j++) {
                int r = row + (horizontal ? 0 : j);
                int c = col + (horizontal ? j : 0);
                if (r < 10 && c < 10) {
                    tempShips[r][c] = false;
                    shipGrid[r][c] = 0;
                }
            }
            shipPlacements.remove(shipPlacements.size() - 1);
        }
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startReplay() {
        if (gameActions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No game actions recorded to replay", "Replay Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        inReplayMode = true;
        currentReplayIndex = 0;

        replayControlPanel.setVisible(true);

        disablePlayerGrid();
        disableOpponentGrid();

        resetGridsForReplay();

        replayStatusLabel.setText("Replay: " + (currentReplayIndex + 1) + "/" + gameActions.size());

        showCurrentAction();

        logMessage("--- REPLAY MODE STARTED ---");
    }

    private void resetGridsForReplay() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                playerGrid[i][j].setText("");
                opponentGrid[i][j].setText("");

                if (initialPlayerShips[i][j]) {
                    int shipType = shipGrid[i][j] - 1;
                    if (shipType >= 0 && shipType < shipColors.length) {
                        playerGrid[i][j].setBackground(shipColors[shipType]);
                    } else {
                        playerGrid[i][j].setBackground(Color.GRAY);
                    }
                } else {
                    playerGrid[i][j].setBackground(UIManager.getColor("Button.background"));
                }

                opponentGrid[i][j].setBackground(UIManager.getColor("Button.background"));
            }
        }

        for (int i = 0; i < 5; i++) {
            if (i < shipPlacements.size()) {
                int[] placement = shipPlacements.get(i);
                int row = placement[0];
                int col = placement[1];
                playerGrid[row][col].setText(String.valueOf(i + 1));
            }
        }
    }

    private void showCurrentAction() {
        if (currentReplayIndex < 0 || currentReplayIndex >= gameActions.size()) {
            return;
        }

        GameAction action = gameActions.get(currentReplayIndex);

        switch (action.type) {
            case PLAYER_SHOT:
                highlightReplayAction(action);
                break;

            case OPPONENT_SHOT:
                highlightReplayAction(action);
                break;

            case GAME_OVER:
                logMessage("Replay: Game Over - " + action.result);
                break;

            case OPPONENT_DISCONNECTED:
                logMessage("Replay: Opponent disconnected");
                break;
        }

        replayStatusLabel.setText("Replay: " + (currentReplayIndex + 1) + "/" + gameActions.size());
    }

    private void showPreviousAction() {
        if (currentReplayIndex > 0) {
            currentReplayIndex--;

            resetGridsForReplay();
            for (int i = 0; i <= currentReplayIndex; i++) {
                replayAction(gameActions.get(i), false);
            }

            replayStatusLabel.setText("Replay: " + (currentReplayIndex + 1) + "/" + gameActions.size());
            highlightReplayAction(gameActions.get(currentReplayIndex));
        }
    }

    private void showNextAction() {
        if (currentReplayIndex < gameActions.size() - 1) {
            currentReplayIndex++;
            GameAction action = gameActions.get(currentReplayIndex);

            replayAction(action, true);

            replayStatusLabel.setText("Replay: " + (currentReplayIndex + 1) + "/" + gameActions.size());
        }
    }

    private void replayAction(GameAction action, boolean highlight) {
        switch (action.type) {
            case PLAYER_SHOT:
                if (action.result.equals("HIT") || action.result.equals("SUNK")) {
                    opponentGrid[action.row][action.col].setBackground(Color.RED);
                    opponentGrid[action.row][action.col].setText("✗");
                    opponentGrid[action.row][action.col].setForeground(Color.WHITE);
                } else {
                    opponentGrid[action.row][action.col].setBackground(Color.BLUE);
                    opponentGrid[action.row][action.col].setText("○");
                    opponentGrid[action.row][action.col].setForeground(Color.WHITE);
                }
                break;

            case OPPONENT_SHOT:
                if (action.result.equals("HIT") || action.result.equals("SUNK")) {
                    playerGrid[action.row][action.col].setBackground(new Color(255, 50, 50));
                    playerGrid[action.row][action.col].setText("✗");
                    playerGrid[action.row][action.col].setForeground(Color.WHITE);
                } else {
                    playerGrid[action.row][action.col].setBackground(new Color(100, 180, 255));
                    playerGrid[action.row][action.col].setText("○");
                    playerGrid[action.row][action.col].setForeground(Color.WHITE);
                }
                break;
        }

        if (highlight) {
            highlightReplayAction(action);
        }
    }

    private void highlightReplayAction(GameAction action) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                playerGrid[i][j].setBorder(UIManager.getBorder("Button.border"));
                opponentGrid[i][j].setBorder(UIManager.getBorder("Button.border"));
            }
        }

        switch (action.type) {
            case PLAYER_SHOT:
                opponentGrid[action.row][action.col].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                logMessage("Replay: Player fired at (" + (char) ('A' + action.col) + (action.row + 1) + ") - " + action.result);
                break;

            case OPPONENT_SHOT:
                playerGrid[action.row][action.col].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                logMessage("Replay: Opponent fired at (" + (char) ('A' + action.col) + (action.row + 1) + ") - " + action.result);
                break;
        }
    }

    private void exitReplay() {
        inReplayMode = false;
        stopAutoReplay();

        replayControlPanel.setVisible(false);

        resetGridsForReplay();

        for (GameAction action : gameActions) {
            replayAction(action, false);
        }

        logMessage("--- REPLAY MODE ENDED ---");
    }

    private enum GameActionType {
        PLAYER_SHOT,
        OPPONENT_SHOT,
        GAME_OVER,
        OPPONENT_DISCONNECTED
    }

    private class GameAction {
        final GameActionType type;
        final int row;
        final int col;
        final String result;

        GameAction(GameActionType type, int row, int col, String result) {
            this.type = type;
            this.row = row;
            this.col = col;
            this.result = result;
        }

        GameAction(GameActionType type, String result) {
            this.type = type;
            this.row = -1;
            this.col = -1;
            this.result = result;
        }

        GameAction(GameActionType type) {
            this.type = type;
            this.row = -1;
            this.col = -1;
            this.result = "";
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            Client client = new Client("localhost", 8080);
            
            client.setVisible(true);
        });
    }
}
