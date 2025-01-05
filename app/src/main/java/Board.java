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
    private int playerCount;
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
        playerCount = numPlayers;
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

    public Map<String, Object> makeMove(JsonNode movesNode) {
        List<Map<String, Object>> moves = new ArrayList<>();
        System.out.println(movesNode);
        // Convert JsonNode to List<Map<String, Object>>
        if (movesNode.isArray()) {
            System.out.println("has moves");
            for (JsonNode moveNode : movesNode) {
                Map<String, Object> move = new HashMap<>();
                move.put("x", moveNode.get("col").asInt());
                move.put("y", moveNode.get("row").asInt());
                
                // Create Piece from JSON data
                // JsonNode pieceNode = moveNode.get("piece");
                String[] name = moveNode.get("piece").asText().split(" ");
                Piece piece = new Piece(name);
                move.put("piece", piece);
                
                moves.add(move);
            }
        }
        
        // Use existing logic with converted moves
        return makeMove(moves);
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getScore(int playerIndex) {
        Player player = switch (playerIndex) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree;
            case 3 -> playerFour;
            default -> throw new IllegalStateException("Invalid turn state");
        };
        return player.getScore();
    }
    
    /**
     * Makes a move with multiple pieces
     * @param moves List of moves, where each move contains x, y coordinates and a piece
     * @return true if the move was successful, false otherwise
     */
    public Map<String, Object> makeMove(List<Map<String, Object>> moves) {
        Map<String, Object> response = new HashMap<>();
        if(moves.isEmpty()) {
            response.put("success", false);
            response.put("error", "No moves provided");
            return response;
        }
        // Get current player
        Player currentPlayer = switch (turn % playerCount) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree;
            case 3 -> playerFour;
            default -> throw new IllegalStateException("Invalid turn state");
        };
        if(!board.isEmpty()) {
            // First validate all moves
            for (Map<String, Object> move : moves) {
                int x = (int) move.get("x");
                int y = (int) move.get("y");
                Piece piece = (Piece) move.get("piece");

                // Check if the position is already occupied
                if (findPieceAt(x, y) != null) {
                    response.put("success", false);
                    response.put("error", "Position already occupied");
                    return response;
                }

                // Check if player has the piece in their hand

                if (!currentPlayer.hasPiece(piece)) {
                    response.put("success", false);
                    response.put("error", "Player does not have the piece in their hand");
                    return response;
                }
            }
        } else if (!isValidFirstMoves(moves)) {
            response.put("success", false);
            response.put("error", "Invalid first move");
            return response;
        }
        // If all moves are valid, execute them
        try {
            for (Map<String, Object> move : moves) {
                int x = (int) move.get("x");
                int y = (int) move.get("y");
                Piece piece = (Piece) move.get("piece");
                // Validate the move according to game rules
                if (!board.isEmpty() && !isValidMove(x, y, piece)) {
                    response.put("success", false);
                    response.put("error", "Invalid move");
                    return response;
                }
                // Place the piece on the board
                board.put(x + ", " + y, piece);
                piece.xCoord = x;
                piece.yCoord = y;

                // Remove the piece from player's hand
                currentPlayer.removePiece(piece);
            }
            response.put("success", true);
            // Calculate score for the moves
            int moveScore = calculateMoveScore(moves.stream()
                                                .map(m -> (Piece)m.get("piece"))
                                                .collect(Collectors.toList()));
            System.out.println("Score Calculated");
            // Add score to current player
            currentPlayer.addScore(moveScore);
            updateDimensions();
            // Draw new tiles
            if(!bag.isEmpty()) {
                List<Piece> Pieces = bag.draw(moves.size());
                System.out.println("Pieces: " + Pieces);
                currentPlayer.addPieces(Pieces);
            } else if(bag.isEmpty() && currentPlayer.getHand().isEmpty()) {
                currentPlayer.addScore(6);
            }
            System.out.println("Pieces Drawn");
            // Increment turn
            response.put("gameState", getGameState());
            passTurn();
            System.out.println("Everything ready");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error making moves: " + e.getMessage());
            return response;
        }
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

    private void updateDimensions() {
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (String key : board.keySet()) {
            String[] coords = key.split(", ");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);

            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        width = maxX + 1;
        height = maxY + 1;
    }

    /**
     * Gets the current game state to send back to client
     */
    public Map<String, Object> getGameState() {
        Map<String, Object> gameState = new HashMap<>();
        
        // Add current board state
        gameState.put("board", getBoardState());
        System.out.println("Boardstate done");
        // Add current player's hand
        Player currentPlayer = switch (turn % playerCount) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree;
            case 3 -> playerFour;
            default -> throw new IllegalStateException("Invalid turn state");
        };
        gameState.put("hand", getCurrentHand());
        System.out.println(getHand(1));
        gameState.put("nextHand", getHand((turn % playerCount) + 1));
        System.out.println("hand done");
        // Add game status information
        gameState.put("turn", turn);
        gameState.put("score", currentPlayer.getScore());
        gameState.put("remainingTiles", bag.size());
        gameState.put("width", width);
        gameState.put("height", height);
        gameState.put("gameOver", gameOver());
        System.out.println(gameState);
        return gameState;
    }

    /**
     * Validates if a move is legal according to Qwirkle rules
     */
    private boolean isValidMove(int x, int y, Piece piece) {
        System.out.println("Board: " + formatBoard());
        System.out.println("Piece location: "+ x + ", " + y);
        // Get adjacent pieces
        List<Piece> adjacentPieces = getAdjacentPieces(x, y);
        if(adjacentPieces.isEmpty()) {
            System.out.println("No Adjacent Pieces");
            return false;
        }
        // Check if placement creates valid lines
        for (Piece adjacent : adjacentPieces) {
            if (!isValidConnection(piece, adjacent)) {
                System.out.println("invalid connection");
                return false;
            }
        }
        
        // Check for duplicate pieces in lines
        if (hasDuplicatesInLine(x, y, piece)) {
            System.out.println("duplicate in line");
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
        return newPiece.equals(existingPiece);
    }

    public void passTurn() {
        turn++;
    }

    /**
     * Gets the current state of the board as a 2D array
     */
    private Piece[][] getBoardState() {
        Piece[][] boardState = new Piece[height][width];
        // Implementation to convert linked piece structure to 2D array
        // This would involve traversing from overallRoot and mapping positions
        for(String coordinates : board.keySet()) {
            String[] coords = coordinates.split(",");
            int x = Integer.valueOf(coords[0].trim());
            int y = Integer.valueOf(coords[1].trim());
            boardState[y][x] = board.get(coordinates);
        }
        return boardState;
    }
    /**
     * Checks if placing a piece would create duplicate pieces in a line
     */
    private boolean hasDuplicatesInLine(int x, int y, Piece piece) {
        for(int i = 0; i < x + 5; i++) {
            if(i != x) {
                Piece p = findPieceAt(i, y);
                if(p == null) {
                    break;
                }
                if(p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
                    return true;
                }
            }
        }
        for(int i = 0; i > x - 5; i--) {
            if(i != x) {
                Piece p = findPieceAt(i, y);
                if(p == null) {
                    break;
                }
                if(p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
                    return true;
                }
            }
        }
        for(int i = 0; i < y + 5; i++) {
            if(i != y) {
                Piece p = findPieceAt(x, i);
                if(p == null) {
                    break;
                }
                if(p != null && p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
                    return true;
                }
            }
        }
        for(int i = 0; i > y - 5; i--) {
            if(i != y) {
                Piece p = findPieceAt(x, i);
                if(p == null) {
                    break;
                }
                if(p.name[0].equals(piece.name[0]) && p.name[1].equals(piece.name[1])) {
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
        Player currentPlayer = switch (playerIndex % 4) {
            case 0 -> playerOne;
            case 1 -> playerTwo;
            case 2 -> playerThree != null ? playerThree : playerOne;
            case 3 -> playerFour != null ? playerFour : playerTwo;
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
            String symbol = tile.name[1];
            pieceData.put("shape", symbol);
            
            formattedHand.add(pieceData);
        }
        
        return formattedHand;
    }
    private List<Map<String, String>> formatBoard() {
        List<Map<String, String>> formattedHand = new ArrayList<>();

        for (String coords : board.keySet()) {
            Map<String, String> pieceData = new HashMap<>();
            Piece tile = board.get(coords);
            // Convert the tile's color to a CSS-compatible color
            String cssColor = tile.name[0].toLowerCase();
            pieceData.put("color", cssColor);
            
            // Convert the tile's shape to a symbol
            String symbol = tile.name[1];
            pieceData.put("shape", symbol);
            pieceData.put("coords", coords);
            
            formattedHand.add(pieceData);
        }
        
        return formattedHand;
    }
    
    public int getTurn() {
        return turn;
    }

    public List<Map<String, String>> formatList(List<Piece> list) {
        List<Map<String, String>> formattedList = new ArrayList<>();
        
        for (Piece tile : list) {
            Map<String, String> pieceData = new HashMap<>();
            
            // Convert the tile's color to a CSS-compatible color
            String cssColor = tile.name[0].toLowerCase();
            pieceData.put("color", cssColor);
            
            // Convert the tile's shape to a symbol
            String symbol = tile.name[1];
            pieceData.put("shape", symbol);

            pieceData.put("coords", tile.xCoord + ", " + tile.yCoord);
            
            formattedList.add(pieceData);
        }
        
        return formattedList;
    }
    public boolean gameOver() {
        if(playerThree == null) {
            return bag.isEmpty() && (playerOne.getHand().isEmpty() || playerTwo.getHand().isEmpty());
        } else if(playerFour == null) {
            return bag.isEmpty() && (playerOne.getHand().isEmpty() || playerTwo.getHand().isEmpty()
            || playerThree.getHand().isEmpty());
        } else {
            return bag.isEmpty() && (playerOne.getHand().isEmpty() || playerTwo.getHand().isEmpty() 
            || playerThree.getHand().isEmpty() || playerFour.getHand().isEmpty());
        }
    }

    public int calculateMoveScore(List<Piece> moves) {
        List<Map<String, String>> formatMoves = formatList(moves);
        System.out.println("Moves: " + formatMoves);
        System.out.println("Board: " + formatBoard());
        int totalScore = 0;
        if(moves.size() == 1 ) {
            for (Piece piece : moves) {
                int vertScore = calculateLineScore(piece, moves, false);
    
                int horiScore = calculateLineScore(piece, moves, true);
                System.out.println("vertScore: " + vertScore);
                System.out.println("horiScore: " + horiScore);
                totalScore += vertScore + horiScore;
            }
        } else if((moves.get(0).yCoord != moves.get(1).yCoord + 1 || moves.get(0).yCoord != moves.get(1).yCoord - 1) || 
            (moves.get(0).xCoord != moves.get(1).xCoord + 1 || moves.get(0).xCoord != moves.get(1).xCoord - 1)) {
            int vertScore = calculateApartLineScore(moves, false);

            int horiScore = calculateApartLineScore(moves, true);
            System.out.println("vertScore: " + vertScore);
            System.out.println("horiScore: " + horiScore);
            totalScore += vertScore + horiScore;
        } else {
            for (Piece piece : moves) {
                int vertScore = calculateLineScore(piece, moves, false);
    
                int horiScore = calculateLineScore(piece, moves, true);
                System.out.println("vertScore: " + vertScore);
                System.out.println("horiScore: " + horiScore);
                totalScore += vertScore + horiScore;
            }
        }
        return totalScore;
    }

    public int calculateLineScore(Piece piece, List<Piece> moves, boolean horizontal) {
        System.out.println("Piece: " + piece.name[0] + ", " + piece.name[1] + 
            " Location: " + piece.xCoord + ", " + piece.yCoord);
        System.out.println((horizontal ? "Horizontal" : "Vertical") + " line");
        int col = piece.xCoord;
        int row = piece.yCoord;
        boolean delete = true;
        Set<Piece> line = new HashSet<>();
        line.add(piece);
        System.out.println("Positive");
        for(int i = 1; i < 5; i++) {
            int checkCol = horizontal ? col + i : col;
            int checkRow = horizontal ? row : row + i;
            System.out.println("checkCol: " + checkCol + " checkRow: " + checkRow);
            System.out.println("Score: " + line.size());
            if(board.get(checkCol + ", " + checkRow) == null) {
                break;
            }
            if(moves.contains(board.get(checkCol + ", " + checkRow))) {
                delete = false;
                break;
            }
            line.add(board.get(checkCol + ", " + checkRow));
        }
        System.out.println("Negative");
        for(int i = 1; i < 5; i++) {
            int checkCol = horizontal ? col - i : col;
            int checkRow = horizontal ? row : row - i;
            System.out.println("checkCol: " + checkCol + " checkRow: " + checkRow);
            System.out.println("Score: " + line.size());
            if(board.get(checkCol + ", " + checkRow) == null) {
                break;
            }
            if(moves.contains(board.get(checkCol + ", " + checkRow))) {
                delete = false;
                break;
            }
            line.add(board.get(checkCol + ", " + checkRow));
        }
        if(line.size() == 1 && delete) {
            line.clear();
        }
        return line.size();
    }

    public int calculateApartLineScore(List<Piece> moves, boolean horizontal) {
        boolean apart = false;
        int totalScore = 0;
        for(Piece piece : moves) {
            System.out.println("Piece: " + piece.name[0] + ", " + piece.name[1] + 
                " Location: " + piece.xCoord + ", " + piece.yCoord);
            System.out.println((horizontal ? "Horizontal" : "Vertical") + " line");
            int col = piece.xCoord;
            int row = piece.yCoord;
            boolean delete = true;
            Set<Piece> line = new HashSet<>();
            line.add(piece);
            Piece temp = piece;
            System.out.println("Positive");
            for(int i = 1; i < 5; i++) {
                int checkCol = horizontal ? col + i : col;
                int checkRow = horizontal ? row : row + i;
                System.out.println("checkCol: " + checkCol + " checkRow: " + checkRow);
                System.out.println("Score: " + line.size());
                if(board.get(checkCol + ", " + checkRow) == null) {
                    break;
                }
                Piece add = board.get(checkCol + ", " + checkRow);
                if(moves.contains(add)) {
                    delete = false;
                    if(!apart && (horizontal ? (temp.xCoord != add.xCoord + 1 || temp.xCoord != add.xCoord - 1) 
                        : (temp.yCoord != add.yCoord + 1 || temp.yCoord != add.yCoord - 1))) {
                        apart = true;
                    } else if(apart) {
                        line.clear();
                        line.add(piece);
                    }
                    break;
                }
                line.add(add);
                temp = add;
            }
            System.out.println("Negative");
            for(int i = 1; i < 5; i++) {
                int checkCol = horizontal ? col - i : col;
                int checkRow = horizontal ? row : row - i;
                System.out.println("checkCol: " + checkCol + " checkRow: " + checkRow);
                System.out.println("Score: " + line.size());
                if(board.get(checkCol + ", " + checkRow) == null) {
                    break;
                }
                Piece add = board.get(checkCol + ", " + checkRow);
                if(moves.contains(add)) {
                    delete = false;
                    if(!apart && (horizontal ? (temp.xCoord != add.xCoord + 1 || temp.xCoord != add.xCoord - 1)
                        : (temp.yCoord != add.yCoord + 1 || temp.yCoord != add.yCoord - 1))) {
                        apart = true;
                    } else if(apart) {
                        line.clear();
                        line.add(piece);
                    }
                    break;
                }
                line.add(add);
                temp = add;
            }
            if(line.size() == 1 && delete) {
                line.clear();
            }
            totalScore += line.size();
        }
        return totalScore;
    }
}
