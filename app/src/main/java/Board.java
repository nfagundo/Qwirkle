import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;
public class Board {
    private int width;
    private int height;
    private Player playerOne;
    private Player playerTwo;
    private Player playerThree;
    private Player playerFour;
    private int turn;
    private Bag bag;
    private Map<String, Piece> board;
    public Board(int numPlayers) throws IllegalArgumentException {
        if(numPlayers < 2 || numPlayers > 4) {
            throw new IllegalArgumentException("Invalid Number of Players!");
        }
        width = 0;
        height = 0;
        turn = 0;
        board = new HashMap<>();
        bag = new Bag();
        if(numPlayers >= 2) {
            playerOne = new Player(bag.draw(6));
            playerTwo = new Player(bag.draw(6));
        }
        if(numPlayers >= 3) {
            playerThree = new Player(bag.draw(6));
        }
        if(numPlayers == 4) {
            playerFour = new Player(bag.draw(6));  
        }
    }
    /**
 * Makes a move with multiple pieces
 * @param moves List of moves, where each move contains x, y coordinates and a piece
 * @return true if the move was successful, false otherwise
 */
public boolean makeMove(List<Map<String, Object>> moves) {
    // Get current player
    Player currentPlayer = switch (turn % 4) {
        case 0 -> playerOne;
        case 1 -> playerTwo;
        case 2 -> playerThree != null ? playerThree : playerOne;
        case 3 -> playerFour != null ? playerFour : playerTwo;
        default -> throw new IllegalStateException("Invalid turn state");
    };

    // First validate all moves
    for (Map<String, Object> move : moves) {
        int x = (int) move.get("x");
        int y = (int) move.get("y");
        Piece piece = (Piece) move.get("piece");

        // Check if the position is already occupied
        if (findPieceAt(x, y) != null) {
            return false;
        }

        // Validate the move according to game rules
        if (!board.isEmpty() && !isValidMove(x, y, piece)) {
            return false;
        }

        // Check if player has the piece in their hand
        if (!currentPlayer.getHand().contains(piece)) {
            return false;
        }
    }

    // If all moves are valid, execute them
    try {
        for (Map<String, Object> move : moves) {
            int x = (int) move.get("x");
            int y = (int) move.get("y");
            Piece piece = (Piece) move.get("piece");

            // Place the piece on the board
            board.put(x + ", " + y, piece);
            piece.xCoord = x;
            piece.yCoord = y;

            // Remove the piece from player's hand
            currentPlayer.getHand().remove(piece);
        }

        // Calculate score for the moves
        int moveScore = calculateMoveScore(moves.stream()
                                              .map(m -> (Piece)m.get("piece"))
                                              .collect(Collectors.toList()));
        
        // Add score to current player
        currentPlayer.addScore(moveScore);

        // Draw new tiles
        while (currentPlayer.getHand().size() < 6 && !bag.isEmpty()) {
            currentPlayer.getHand().add(bag.remove(0));
        }

        // Increment turn
        turn++;

        return true;
    } catch (Exception e) {
        System.err.println("Error making moves: " + e.getMessage());
        return false;
    }
}

/**
 * Validates if all moves are legal according to Qwirkle rules
 */
private boolean isValidMove(List<Map<String, Object>> moves) {
    // If this is the first move on the board, only need to check if pieces are valid together
    if (board.isEmpty()) {
        return isValidFirstMoves(moves);
    }

    // Check if at least one piece connects to existing pieces
    boolean hasConnection = false;
    for (Map<String, Object> move : moves) {
        int x = (int) move.get("x");
        int y = (int) move.get("y");
        List<Piece> adjacent = getAdjacentPieces(x, y);
        if (!adjacent.isEmpty()) {
            hasConnection = true;
            break;
        }
    }

    if (!hasConnection) {
        return false;
    }

    // Validate each move individually
    for (Map<String, Object> move : moves) {
        int x = (int) move.get("x");
        int y = (int) move.get("y");
        Piece piece = (Piece) move.get("piece");

        if (!isValidMove(x, y, piece)) {
            return false;
        }
    }

    return true;
}

/**
 * Validates the first moves of the game
 */
private boolean isValidFirstMoves(List<Map<String, Object>> moves) {
    // Check if pieces form a valid line
    // For simplicity, we'll just check if they're all in the same row or column
    boolean sameRow = moves.stream()
            .map(m -> (int)m.get("y"))
            .distinct()
            .count() == 1;

    boolean sameColumn = moves.stream()
            .map(m -> (int)m.get("x"))
            .distinct()
            .count() == 1;

    if (!sameRow && !sameColumn) {
        return false;
    }

    // Check if pieces follow Qwirkle rules (same color or same shape)
    List<Piece> pieces = moves.stream()
            .map(m -> (Piece)m.get("piece"))
            .collect(Collectors.toList());

    boolean sameColor = pieces.stream()
            .map(p -> p.name[0])
            .distinct()
            .count() == 1;

    boolean sameShape = pieces.stream()
            .map(p -> p.name[1])
            .distinct()
            .count() == 1;

    return sameColor || sameShape;
}

        /**
     * Makes a move in the Qwirkle game based on client input.
     * Expected JSON format from client:
     * {
     *   "pieceIndex": 0,        // Index of piece in player's hand
     *   "x": 0,                 // X coordinate on board
     *   "y": 0,                 // Y coordinate on board
     *   "direction": "right"    // Direction to place piece: up, down, left, right
     * }
     * 
     * @param moveData JsonNode containing the move information from the client
     * @return Map containing move result and updated game state
     */
    public Map<String, Object> makeMove(JsonNode moveData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get current player
            Player currentPlayer = switch (turn % 4) {
                case 0 -> playerOne;
                case 1 -> playerTwo;
                case 2 -> playerThree != null ? playerThree : playerOne;
                case 3 -> playerFour != null ? playerFour : playerTwo;
                default -> throw new IllegalStateException("Invalid turn state");
            };

            // Extract move data
            int pieceIndex = moveData.get("pieceIndex").asInt();
            int x = moveData.get("x").asInt();
            int y = moveData.get("y").asInt();
            String direction = moveData.get("direction").asText().toLowerCase();

            // Validate piece selection
            if (pieceIndex < 0 || pieceIndex >= currentPlayer.getHand().size()) {
                response.put("success", false);
                response.put("error", "Invalid piece selection");
                return response;
            }

            // Get selected piece
            Piece selectedPiece = currentPlayer.getHand().get(pieceIndex);

            // Validate direction
            if (!Arrays.asList("up", "down", "left", "right").contains(direction)) {
                response.put("success", false);
                response.put("error", "Invalid direction");
                return response;
            }

            // Handle first piece placement
            if (board.isEmpty()) {
                if (isValidFirstMove(selectedPiece)) {
                    width = 1;
                    height = 1;
                    board.put("0,0", selectedPiece);
                    updateGameStateAfterMove(currentPlayer, pieceIndex);
                    response.put("success", true);
                    response.put("gameState", getGameState());
                    return response;
                }
                response.put("success", false);
                response.put("error", "Invalid first move");
                return response;
            }

            // Handle subsequent moves
            if (isValidMove(x, y, selectedPiece)) {
                board.put(x + ", " + y, selectedPiece);
                updateGameStateAfterMove(currentPlayer, pieceIndex);
                response.put("success", true);
                response.put("gameState", getGameState());
                return response;
            }

            response.put("success", false);
            response.put("error", "Invalid move");
            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Server error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Updates the game state after a valid move
     */
    private void updateGameStateAfterMove(Player currentPlayer, int pieceIndex) {
        // Remove played piece from hand
        currentPlayer.getHand().remove(pieceIndex);
        
        // Draw new piece if bag isn't empty
        if (!bag.isEmpty()) {
            currentPlayer.getHand().add(bag.draw(1).get(0));
        } 
        // Update board dimensions
        updateDimensions();
        
        // Increment turn
        turn++;
    }

    private void updateDimensions() {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (String key : board.keySet()) {
            String[] coords = key.split(", ");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        width = maxX - minX + 1;
        height = maxY - minY + 1;
    }

    /**
     * Gets the current game state to send back to client
     */
    private Map<String, Object> getGameState() {
        Map<String, Object> gameState = new HashMap<>();
        
        // Add current board state
        gameState.put("board", getBoardState());
        
        // Add current player's hand
        Player currentPlayer = switch (turn % 4) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree != null ? playerThree : playerOne;
            case 3 -> playerFour != null ? playerFour : playerTwo;
            default -> throw new IllegalStateException("Invalid turn state");
        };
        gameState.put("hand", currentPlayer.getHand());
        
        // Add game status information
        gameState.put("turn", turn);
        gameState.put("remainingTiles", bag.size());
        gameState.put("width", width);
        gameState.put("height", height);
        gameState.put("gameOver", gameOver());
        
        return gameState;
    }

    /**
     * Validates if a move is legal according to Qwirkle rules
     */
    private boolean isValidMove(int x, int y, Piece piece) {
        // Get adjacent pieces
        List<Piece> adjacentPieces = getAdjacentPieces(x, y);
        
        // Check if placement creates valid lines
        for (Piece adjacent : adjacentPieces) {
            if (!isValidConnection(piece, adjacent)) {
                return false;
            }
        }
        
        // Check for duplicate pieces in lines
        if (hasDuplicatesInLine(x, y, piece)) {
            return false;
        }
        
        return true;
    }

    private List<Piece> getAdjacentPieces(int x, int y) {
        List<Piece> adjacentPieces = new ArrayList<>();
        Piece left = findPieceAt(x - 1, y);
        Piece right = findPieceAt(x + 1, y);
        Piece up = findPieceAt(x, y - 1);
        Piece down = findPieceAt(x, y + 1);

        if (left != null) adjacentPieces.add(left);
        if (right != null) adjacentPieces.add(right);
        if (up != null) adjacentPieces.add(up);
        if (down != null) adjacentPieces.add(down);

        return adjacentPieces;
    }

    /**
     * Checks if a piece can connect to an adjacent piece according to Qwirkle rules
     */
    private boolean isValidConnection(Piece newPiece, Piece existingPiece) {
        return newPiece.name[0].equals(existingPiece.name[0]) ||
            newPiece.name[1].equals(existingPiece.name[1]);
    }

    /**
     * Gets the current state of the board as a 2D array
     */
    private Piece[][] getBoardState() {
        Piece[][] boardState = new Piece[height][width];
        // Implementation to convert linked piece structure to 2D array
        // This would involve traversing from overallRoot and mapping positions
        for(String coordinates : board.keySet()) {
            String[] coords = coordinates.split(", ");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            boardState[y][x] = board.get(coordinates);
        }
        return boardState;
    }

    /**
     * Validates the first move of the game
     */
    private boolean isValidFirstMove(Piece piece) {
        // First move is always valid in Qwirkle
        return true;
    }

    /**
     * Checks if placing a piece would create duplicate pieces in a line
     */
    private boolean hasDuplicatesInLine(int x, int y, Piece piece) {
        for(int i = x - 5; i < x + 5; i++) {
            if(i != x) {
                Piece p = findPieceAt(i, y);
                if(p != null && p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
                    return true;
                }
            }
        }
        for(int i = y - 5; i < y + 5; i++) {
            if(i != y) {
                Piece p = findPieceAt(x, i);
                if(p != null && p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private Piece findPieceAt(int x, int y) {
        return board.get(x + ", " + y);
    }

    public List<Map<String, String>> getCurrentHand() {
        return getHand(turn);
    }

    public List<Map<String, String>> getHand(int playerIndex) {
        Player currentPlayer = switch (playerIndex) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree;
            case 3 -> playerFour;
            default -> throw new IllegalStateException("Invalid player index");
        };
        List<Map<String, String>> formattedHand = new ArrayList<>();
    
        List<Piece> playerHand = currentPlayer.getHand();
        
        for (Piece tile : playerHand) {
            Map<String, String> pieceData = new HashMap<>();
            
            // Convert the tile's color to a CSS-compatible color
            String cssColor = tile.name[0].toLowerCase();
            pieceData.put("color", cssColor);
            
            // Convert the tile's shape to a symbol
            String symbol = convertShapeToSymbol(tile.name[1]);
            pieceData.put("shape", symbol);
            
            formattedHand.add(pieceData);
        }
        
        return formattedHand;
    }

    private String convertShapeToSymbol(String shape) {
        System.out.println("Input shape: '" + shape + "'");
        System.out.println("Length: " + shape.length());
        System.out.println("Bytes: " + Arrays.toString(shape.getBytes()));
        if(shape.equals("Circle")) {
            return "C";
        } else if(shape.equals("Square")) {
            return "S";
        } else if(shape.equals("Diamond")) {
            return "D";
        } else if(shape.equals("8pt-Star")) {
            return "8";
        } else if(shape.equals("Clover")) {
            return "Q";
        } else if(shape.equals("4pt-Star")) {
            return "4";
        } else {
            return "?";
        }
        // switch (shape) {  // trim() removes leading/trailing whitespace
        //     case "Circle":
        //         System.out.println("●");
        //         return "●";
        //     case "Square": 
        //         System.out.println("■"); return "■";
        //     case "Diamond":
        //         System.out.println("◆");return "◆";
        //     case "8pt-Star":
        //         System.out.println("✦"); return "★";
        //     case "Clover":
        //         System.out.println("♣"); return "♣";
        //     case "4pt-Star":
        //         System.out.println("✚"); return "✚";
        //     default:
        //         System.out.println("?"); return "?";
        // }
    }
    
    public int getTurn() {
        return turn;
    }

    public boolean gameOver() {
        return bag.isEmpty() && (playerOne.getHand().isEmpty() || playerTwo.getHand().isEmpty() || playerThree.getHand().isEmpty() || playerFour.getHand().isEmpty());
    }

    public int calculateMoveScore(List<Piece> moves) {
        int totalScore = 0;
        
        for (Piece move : moves) {
            // Calculate horizontal line score
            int horizontalScore = calculateLineScore(move, true);
            // Calculate vertical line score
            int verticalScore = calculateLineScore(move, false);
            
            // If this is the first move, count the tile only once
            totalScore += (horizontalScore + verticalScore - 
                (horizontalScore > 0 && verticalScore > 0 ? 1 : 0));
        }
        
        return totalScore;
    }
    
    private int calculateLineScore(Piece move, boolean horizontal) {
        List<Piece> line = getLine(move, horizontal);
        
        if (line.size() <= 1) {
            return 0;
        }
        
        int score = line.size();
        
        // Check for Qwirkle (6 tiles)
        if (line.size() == 6) {
            score += 6; // Bonus points for Qwirkle
        }
        
        return score;
    }
    
    private List<Piece> getLine(Piece move, boolean horizontal) {
        List<Piece> line = new ArrayList<>();
        int row = move.xCoord;
        int col = move.yCoord;
        
        // Check in both directions
        for (int i = -5; i <= 5; i++) {
            int checkRow = horizontal ? row : row + i;
            int checkCol = horizontal ? col + i : col;
            
            if (checkRow < 0 || checkRow >= width*height || 
                checkCol < 0 || checkCol >= width*height) {
                continue;
            }
            
            Piece tile = board.get(checkRow + "," + checkCol);
            if (tile == null) {
                if (line.size() > 0) break; // Break if we find a gap after finding tiles
                continue;
            }
            
            line.add(tile);
        }
        
        return line;
    }
}
