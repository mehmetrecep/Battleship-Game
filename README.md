Battleship Game Project Report
1. Introduction
1.1 Project Overview
The Battleship game is a networked, two-player strategy game implemented in Java, where players place ships on a 10x10 grid and take turns firing shots to sink their opponent’s ships. The project aims to create a server-client application that supports real-time gameplay, reliable communication, and robust game state management.
1.2 Objectives

Develop a server to handle multiple concurrent games and client connections.
Implement game logic for ship placement, turn-based shooting, and win/loss conditions.
Ensure reliable communication using TCP for consistent game state updates.
Handle edge cases such as invalid inputs and player disconnections gracefully.

2. System Architecture
2.1 Overview
The system follows a client-server architecture:

Server: Manages client connections, matchmaking, and game instances.
Client: Communicates with the server to send ship placements, shots, and receive game updates.
Game Logic: Encapsulated in the Game class, handling ship placement, shots, and game state.

2.2 Components

Server.java: Uses Java NIO (ServerSocketChannel, Selector) for non-blocking I/O to manage multiple clients. A matchmaking thread pairs players into games.
Player.java: Represents a client, handling TCP communication via SocketChannel for sending/receiving messages.
Game.java: Manages game state for a two-player match, including ship grids, shot tracking, and turn management.
GameAction.java: Defines a structure for game actions (e.g., shots), though not currently used in the implementation.

2.3 Communication Protocol
The protocol uses newline-terminated, pipe-separated (|) text messages over TCP. Key messages include:

GAME_STARTING|opponentName: Initiates a game.
PLACE_SHIPS|row,col,orientation|...: Submits ship placements.
FIRE|row,col: Fires a shot.
SHOT_RESULT|row,col|result: Reports hit, miss, or sunk.
GAME_OVER|message: Signals game end.

3. Implementation Details
3.1 Server Implementation

Connection Handling: The server uses a Selector to manage non-blocking I/O operations (OP_ACCEPT, OP_READ, OP_WRITE). New clients are accepted via ServerSocketChannel and registered as Player objects.
Matchmaking: A dedicated thread checks every 2 seconds for at least two players in a CopyOnWriteArrayList of waiting players, shuffles them, and creates Game instances with unique IDs.
Concurrency: Thread-safe collections (CopyOnWriteArrayList, ConcurrentHashMap) ensure safe access to player and game data.

3.2 Game Logic

Ship Placement: Players submit coordinates for five ships (lengths 5, 4, 3, 3, 2). The handleShipPlacement method validates:
Correct number of ships.
Coordinates within 0-9.
No board overflow or ship overlaps.Valid placements update the player’s 10x10 ship grid; invalid ones trigger INVALID_PLACEMENT messages.


Shooting: The handleShot method processes FIRE messages, validating coordinates and checking the opponent’s ship grid. Hits, misses, or sunk ships are reported via SHOT_RESULT and OPPONENT_SHOT messages.
Sunk Detection: The checkSunk method uses breadth-first search to identify a ship’s cells and confirm if all are hit.
Game End: A game ends when all of one player’s ships are sunk (isPlayerDefeated) or a player disconnects, sending VICTORY, DEFEAT, or OPPONENT_DISCONNECTED messages.

3.3 Player Management

Connection: Each Player uses a SocketChannel for communication, with a ByteBuffer for reading and a Queue for writing messages.
Disconnection Handling: The server detects disconnections via readMessage returning null or I/O exceptions, removing the player and notifying opponents.

4. Challenges and Solutions

Non-blocking I/O: Ensuring the server handles multiple clients without blocking required careful use of Selector and ServerSocketChannel. An initial issue with IllegalBlockingModeException was resolved by ensuring consistent non-blocking configuration.
Ship Placement Validation: Preventing invalid placements (e.g., overlaps, out-of-bounds) required robust checks in handleShipPlacement. Iterative placement was supported by clearing previous placements.
Message Parsing: Handling partial or malformed messages was addressed by buffering incomplete messages in Player.readMessage until a newline is received.
Infinite Loops: Early issues with repetitive player name printing were fixed by ensuring proper game state transitions and message validation.

5. Testing and Validation

Unit Testing: Validation logic in handleShipPlacement and handleShot was tested with edge cases (e.g., invalid coordinates, duplicate shots).
Integration Testing: Simulated client connections verified matchmaking, ship placement, and game flow. Disconnection scenarios were tested to ensure proper notifications.
Debugging: System logs for ship placement errors and disconnections aided in identifying and resolving issues.

6. Outcomes and Achievements

Functional Game: The system supports two-player or more Battleship games with reliable turn-based gameplay and real-time updates.
Scalability: Non-blocking I/O and concurrent collections allow handling multiple games, though further optimization could enhance large-scale performance.
Robustness: The game handles invalid inputs and disconnections gracefully, maintaining a consistent state.


