/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.networkingproj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author moustafa
 */
public class Game {
        private final String gameId;
        private final Player player1;
        private final Player player2;
        private final boolean[][] player1Ships = new boolean[10][10];
        private final boolean[][] player2Ships = new boolean[10][10];
        private final boolean[][] player1Shots = new boolean[10][10];
        private final boolean[][] player2Shots = new boolean[10][10];
        private boolean player1PlacedShips = false;
        private boolean player2PlacedShips = false;
        private boolean player1Turn;
        private final int[] shipLengths = {5, 4, 3, 3, 2};
        private boolean gameOver = false;

        public Game(String gameId, Player player1, Player player2) {
            this.gameId = gameId;
            this.player1 = player1;
            this.player2 = player2;
            this.player1Turn = new Random().nextBoolean();
        }

        public String getGameId() {
            return gameId;
        }

        public boolean isPlayer(Player player) {
            return player == player1 || player == player2;
        }

        public boolean isPlayerTurn(Player player) {
            return (player == player1 && player1Turn) || (player == player2 && !player1Turn);
        }

        public boolean hasActivePlayers() {
            return (player1 != null && player1.isConnected()) ||
                    (player2 != null && player2.isConnected());
        }

        public void start() {
            player1.sendMessage("GAME_STARTING|" + player2.getName());
            player2.sendMessage("GAME_STARTING|" + player1.getName());
            player1.sendMessage("PLACE_YOUR_SHIPS");
            player2.sendMessage("PLACE_YOUR_SHIPS");
        }

        public void handleShipPlacement(Player player, String message) {
            System.out.println("Processing ship placement from " + player.getName() + ": " + message);

            String[] parts = message.split("\\|");
            boolean[][] ships = (player == player1) ? player1Ships : player2Ships;

            if (player == player1 && player1PlacedShips) {
                clearShips(player1Ships);
                player1PlacedShips = false;
            } else if (player == player2 && player2PlacedShips) {
                clearShips(player2Ships);
                player2PlacedShips = false;
            }

            if (parts.length - 1 != shipLengths.length) {
                player.sendMessage("INVALID_PLACEMENT|Wrong number of ships. Expected " + shipLengths.length + ", got " + (parts.length - 1));
                System.out.println("Invalid ship count from " + player.getName() + ": expected " + shipLengths.length + ", got " + (parts.length - 1));
                return;
            }

            boolean valid = true;
            String errorMessage = "";
            List<int[]> placements = new ArrayList<>();

            for (int i = 1; i < parts.length; i++) {
                String[] coords = parts[i].split(",");
                if (coords.length != 3) {
                    valid = false;
                    errorMessage = "Invalid format for ship " + i + ". Expected 'row,col,orientation'";
                    break;
                }

                try {
                    int row = Integer.parseInt(coords[0].trim());
                    int col = Integer.parseInt(coords[1].trim());
                    boolean horizontal = coords[2].trim().equalsIgnoreCase("H");
                    int expectedLength = shipLengths[i - 1];

                    if (row < 0 || row >= 10 || col < 0 || col >= 10) {
                        valid = false;
                        errorMessage = "Ship " + i + " placement out of bounds: " + row + "," + col;
                        break;
                    }

                    if ((horizontal && col + expectedLength > 10) ||
                            (!horizontal && row + expectedLength > 10)) {
                        valid = false;
                        errorMessage = "Ship " + i + " extends beyond the board";
                        break;
                    }

                    boolean hasOverlap = false;
                    for (int j = 0; j < expectedLength; j++) {
                        int r = row + (horizontal ? 0 : j);
                        int c = col + (horizontal ? j : 0);

                        if (ships[r][c]) {
                            hasOverlap = true;
                            errorMessage = "Ship " + i + " overlaps with another ship at " + r + "," + c;
                            break;
                        }
                    }

                    if (hasOverlap) {
                        valid = false;
                        break;
                    }

                    placements.add(new int[]{row, col, horizontal ? 1 : 0, expectedLength});

                } catch (NumberFormatException e) {
                    valid = false;
                    errorMessage = "Invalid coordinates for ship " + i + ": " + parts[i];
                    break;
                }
            }

            if (valid) {
                List<String> shipCells = new ArrayList<>();
                for (int[] placement : placements) {
                    int row = placement[0];
                    int col = placement[1];
                    boolean horizontal = placement[2] == 1;
                    int length = placement[3];

                    for (int j = 0; j < length; j++) {
                        int r = row + (horizontal ? 0 : j);
                        int c = col + (horizontal ? j : 0);
                        ships[r][c] = true;
                        shipCells.add(r + "," + c);
                    }
                }

                if (player == player1) {
                    player1PlacedShips = true;
                } else {
                    player2PlacedShips = true;
                }

                player.sendMessage("SHIPS_PLACED");

                String shipPositions = "SHIP_POSITIONS|" + String.join("|", shipCells);
                System.out.println("Sending to " + player.getName() + ": " + shipPositions);
                player.sendMessage(shipPositions);

                if (player1PlacedShips && player2PlacedShips) {
                    player1.sendMessage("BATTLE_STARTING");
                    player2.sendMessage("BATTLE_STARTING");
                    sendTurnMessage();
                }
            } else {
                player.sendMessage("INVALID_PLACEMENT|" + errorMessage);
                System.out.println("Invalid placement from " + player.getName() + ": " + errorMessage);
            }
        }

        private void clearShips(boolean[][] ships) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    ships[i][j] = false;
                }
            }
        }

        public void handleShot(Player player, String message) {
            if (gameOver) return;

            String[] parts = message.split("\\|");
            if (parts.length < 2) return;
            String[] coords = parts[1].split(",");
            if (coords.length != 2) return;

            try {
                int row = Integer.parseInt(coords[0].trim());
                int col = Integer.parseInt(coords[1].trim());

                boolean[][] opponentShips = (player == player1) ? player2Ships : player1Ships;
                boolean[][] shots = (player == player1) ? player1Shots : player2Shots;
                Player opponent = (player == player1) ? player2 : player1;

                if (row < 0 || row >= 10 || col < 0 || col >= 10 || shots[row][col]) {
                    return;
                }

                shots[row][col] = true;
                String result;

                if (opponentShips[row][col]) {
                    result = checkSunk(opponentShips, shots, row, col) ? "SUNK" : "HIT";
                } else {
                    result = "MISS";
                }

                player.sendMessage("SHOT_RESULT|" + row + "," + col + "|" + result);
                if (opponent.isConnected()) {
                    opponent.sendMessage("OPPONENT_SHOT|" + row + "," + col + "|" + result);
                }

                if (isPlayerDefeated(opponentShips, shots)) {
                    gameOver = true;
                    player.sendMessage("VICTORY");
                    if (opponent.isConnected()) {
                        opponent.sendMessage("DEFEAT");
                    }

                    sendGameOverMessage(player);
                    if (opponent.isConnected()) {
                        sendGameOverMessage(opponent);
                    }

                    return;
                }

                player1Turn = !player1Turn;
                sendTurnMessage();
            } catch (NumberFormatException e) {
            }
        }

        private void sendGameOverMessage(Player player) {
            player.sendMessage("GAME_OVER|Type FIND_GAME to play again");
        }

        private boolean checkSunk(boolean[][] ships, boolean[][] shots, int row, int col) {
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            List<int[]> shipCells = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Queue<int[]> queue = new LinkedList<>();
            queue.add(new int[]{row, col});
            visited.add(row + "," + col);

            while (!queue.isEmpty()) {
                int[] cell = queue.poll();
                shipCells.add(cell);
                for (int[] dir : directions) {
                    int r = cell[0] + dir[0];
                    int c = cell[1] + dir[1];
                    String key = r + "," + c;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10 && ships[r][c] && !visited.contains(key)) {
                        queue.add(new int[]{r, c});
                        visited.add(key);
                    }
                }
            }

            for (int[] cell : shipCells) {
                if (!shots[cell[0]][cell[1]]) {
                    return false;
                }
            }
            return true;
        }

        private boolean isPlayerDefeated(boolean[][] ships, boolean[][] shots) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (ships[i][j] && !shots[i][j]) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void sendTurnMessage() {
            if (gameOver) return;

            if (player1Turn && player1.isConnected()) {
                player1.sendMessage("YOUR_TURN");
                if (player2.isConnected()) {
                    player2.sendMessage("OPPONENT_TURN");
                }
            } else if (!player1Turn && player2.isConnected()) {
                player2.sendMessage("YOUR_TURN");
                if (player1.isConnected()) {
                    player1.sendMessage("OPPONENT_TURN");
                }
            }
        }

        public void handleDisconnect(Player disconnectedPlayer) {
            Player remainingPlayer = (disconnectedPlayer == player1) ? player2 : player1;

            if (remainingPlayer != null && remainingPlayer.isConnected()) {
                remainingPlayer.sendMessage("OPPONENT_DISCONNECTED");
                sendGameOverMessage(remainingPlayer);
            }

            gameOver = true;
        }
    }
