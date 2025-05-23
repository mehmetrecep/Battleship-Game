
## Battleship Game


A networked, two-player or more Battleship game implemented in Java, where players place ships on a 10x10 grid and take turns firing shots to sink their opponent's fleet. The project uses a client-server architecture with TCP communication for real-time gameplay.
Features

Multiplayer: Supports two players per game with automatic matchmaking.
Reliable Communication: Uses TCP for consistent game state updates.
Robust Game Logic: Validates ship placements and shots, handles hits, misses, and sunk ships.
Logging: Records server, player, and game events to log files for debugging.
Console Client: Provides a text-based interface for gameplay.

## Project Structure

Server: Manages client connections and game instances using Java NIO (Server.java, Player.java, Game.java).
Client: Connects to the server and handles user input (Client.java).
Game Logic: Encapsulates ship placement, shooting, and win conditions (Game.java, GameAction.java, GameActionType.java).

## Prerequisites

Java Development Kit (JDK) 8 or higher.
Git for cloning the repository.
Terminal emulator (e.g., Telnet) or the provided client for gameplay.

## Setup Instructions
1. Clone the Repository
git clone https://github.com/mehmetrecep/Battleship-Game
cd battleship-repo

2. Compile the Project
javac Server.java Player.java Game.java GameAction.java GameActionType.java Client.java

3. Run the Server
java Server 8080


The server listens on port 8080 (or a specified port).

4. Run the Client
Start a client instance for each player:
java Client localhost 8080


Connects to the server at localhost:8080.

## Gameplay

Join the Game:

Launch the client and enter a name when prompted.
Wait for an opponent (matchmaking pairs players every 2 seconds).


Place Ships:

Enter coordinates for five ships (Carrier: 5, Battleship: 4, Cruiser: 3, Submarine: 3, Destroyer: 2).
Format: row,col,H|V (e.g., 0,0,H for horizontal at (0,0)).
Example: 0,0,H|2,2,V|4,4,H|6,6,V|8,8,H.


Play:

Take turns firing shots by entering coordinates (e.g., 2,3).
Receive feedback: HIT, MISS, or SUNK.
The game ends when all opponent ships are sunk (VICTORY or DEFEAT).


Quit or Restart:

Type QUIT to exit.
Type FIND_GAME to join a new match.



Protocol
The client and server communicate via newline-terminated, pipe-separated text messages over TCP:

GAME_STARTING|opponentName: Game begins.
PLACE_SHIPS|row,col,orientation|...: Ship placement.
FIRE|row,col: Fire a shot.
SHOT_RESULT|row,col|result: Shot outcome (HIT, MISS, SUNK).
GAME_OVER|message: Game ends.

Logging

Server: Logs to server.log.
Player: Logs to player_<address>.log.
Game: Logs to game_<gameId>.log.
Client: Logs to client_<timestamp>.log.

Future Improvements

Graphical user interface (GUI) for visual gameplay.
Reconnection support for interrupted games.
Database for game state persistence.
Authentication and encryption for secure communication.

## Contributing

Fork the repository.
Create a feature branch: 
git checkout -b feature/your-feature.
Commit changes: git commit -m "Add your feature".
Push to GitHub: git push origin feature/your-feature.
Open a pull request.

  
