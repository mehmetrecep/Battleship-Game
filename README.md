
## Battleship Game


A networked, two-player or more Battleship game implemented in Java, where players place ships on a 10x10 grid and take turns firing shots to sink their opponent's fleet. The project uses a client-server architecture with TCP communication for real-time gameplay.
Features

Multiplayer: Supports two players per game with automatic matchmaking.
Reliable Communication: Uses TCP for consistent game state updates.
Robust Game Logic: Validates ship placements and shots, handles hits, misses, and sunk ships.
Logging: Records server, player, and game events to log files for debugging.
Console Client: Provides a text-based interface for gameplay.

## Project Structure

Server: Manages client connections and game instances using Java NIO(Non-blocking IO) (Server.java, Player.java, Game.java).
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

## To connect to the server
<img width="885" alt="Screenshot 2025-05-23 at 20 47 28" src="https://github.com/user-attachments/assets/d45d72a1-3301-4433-81e8-8932af45375b" />

## To Enter your name
<img width="885" alt="Screenshot 2025-05-23 at 20 48 40" src="https://github.com/user-attachments/assets/8b1e8c7b-a82e-459b-b7bb-02e595f85ee3" />

## No. of waiting players
<img width="885" alt="Screenshot 2025-05-23 at 20 49 34" src="https://github.com/user-attachments/assets/dae31347-8136-4e20-8c6d-2a1b77f73363" />

<b>Place Ships</b>:

Enter coordinates for five ships (Carrier: 5, Battleship: 4, Cruiser: 3, Submarine: 3, Destroyer: 2).
By placing one cell, it will place the ship horizontally or vertically. You rotate the position of the ship by clicking <b>Rotate ship</b>

<img width="903" alt="Screenshot 2025-05-23 at 20 53 31" src="https://github.com/user-attachments/assets/c3fb6064-058e-411b-bc9d-63b02adaad5e" />

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

  
