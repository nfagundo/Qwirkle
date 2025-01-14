import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GameServer {

    private static Board gameBoard = null;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) throws Exception {
        try {
            // Print working directory for debugging
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
            
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            
            // Add CORS headers to allow local development
            server.createContext("/", new FileHandler());
            server.createContext("/api/initialize", new InitializeHandler());
            server.createContext("/api/move", new MoveHandler());
            server.createContext("/api/calculate-score", new QwirkleController());
            server.createContext("/api/pass", new PassHandler());
            server.createContext("/api/redraw", new RedrawHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server started at http://localhost:8080");
        } catch (Exception e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("src/main/html/index.html");
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        }
    }

    static class MoveHandler implements HttpHandler {
        // Remove the board initialization from here
        public MoveHandler() {
            // Empty constructor now
        }
    
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Check if game is initialized
                if (gameBoard == null) {
                    String errorResponse = "{\"error\": \"Game not initialized\"}";
                    byte[] errorBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, errorBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                    }
                    return;
                }
        
                // Parse the move data from the request
                ObjectMapper mapper = new ObjectMapper();
                JsonNode moveData = mapper.readTree(exchange.getRequestBody());
                Map<String, Object> result = gameBoard.makeMove(moveData);
                
                // Convert response to JSON and get bytes
                String response = mapper.writeValueAsString(result);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                
                // Send response with correct content length
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                
            } catch (Exception e) {
                // Handle errors
                String errorResponse = "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
                byte[] errorBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }
    }
    static class QwirkleController implements HttpHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
                return;
            }
    
            try {
                // Add debug logging
                System.out.println("Calculating score for moves...");
                
                // Read the request body
                InputStream requestBody = exchange.getRequestBody();
                List<Piece> moves = objectMapper.readValue(
                    requestBody, 
                    new TypeReference<List<Piece>>() {}
                );
    
                // Debug log received moves
                System.out.println("Received moves: " + moves);
    
                if (moves == null || moves.isEmpty()) {
                    sendErrorResponse(exchange, 400, "No moves provided");
                    return;
                }
    
                // Calculate score
                int score = gameBoard.calculateMoveScore(moves);
    
                // Prepare response
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("score", score);
                response.put("currentPlayer", gameBoard.getTurn());
    
                // Convert response to JSON
                String jsonResponse = objectMapper.writeValueAsString(response);
                byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
    
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
    
            } catch (Exception e) {
                System.err.println("Error processing score calculation: " + e.getMessage());
                e.printStackTrace();
                
                // Handle errors
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
    
                String jsonError = objectMapper.writeValueAsString(errorResponse);
                byte[] errorBytes = jsonError.getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }
    
        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", message);
    
            String jsonError = objectMapper.writeValueAsString(errorResponse);
            byte[] errorBytes = jsonError.getBytes(StandardCharsets.UTF_8);
    
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
    

    static class InitializeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            try {
                // Read the request body
                InputStream requestBody = exchange.getRequestBody();
                String body = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Received body: " + body);
                JsonNode requestData = objectMapper.readTree(body);
                
                int playerCount = requestData.get("playerCount").asInt();
                System.out.println("Player count: " + playerCount);
                // Validate player count
                if (playerCount < 2 || playerCount > 4) {
                    sendResponse(exchange, 400, "Invalid player count. Must be between 2 and 4");
                    return;
                }

                // Initialize the game board with the player count
                gameBoard = new Board(playerCount);

                // Create response with initial game state
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("playerCount", playerCount);
                response.put("initialHand", gameBoard.getHand(0)); // Get first player's hand
                response.put("currentPlayer", 1);

                // Send success response
                String jsonResponse = objectMapper.writeValueAsString(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.length());
                System.out.println("Sent response: " + jsonResponse);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }

            } catch (Exception e) {
                sendResponse(exchange, 500, "Error initializing game: " + e.getMessage());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("success", statusCode == 200);
            response.put("message", message);
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, jsonResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }
    }

    static class PassHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
    
            try {
                // Check if game is initialized
                if (gameBoard == null) {
                    sendErrorResponse(exchange, 400, "Game not initialized");
                    return;
                }
                Map<String, Object> response = new HashMap<>();
                response.put("gameState", gameBoard.getGameState());
                
                // Pass turn to next player
                gameBoard.passTurn();
                
                // Get new hand for next player
                List<Map<String, String>> newHand = gameBoard.getHand(gameBoard.getTurn());
    
                // Create response object
                response.put("success", true);
                response.put("newHand", newHand);
                
                // Add game state information
                Map<String, Object> gameState = new HashMap<>();
                gameState.put("currentPlayer", gameBoard.getTurn());
                
                // Add scores for all players
                Map<Integer, Integer> scores = new HashMap<>();
                for (int i = 0; i < gameBoard.getPlayerCount(); i++) {
                    scores.put(i + 1, gameBoard.getScore(i));
                }
                gameState.put("scores", scores);
                
                response.put("gameState", gameState);
    
                // Convert to JSON and send response
                String jsonResponse = objectMapper.writeValueAsString(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
    
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Error processing pass: " + e.getMessage());
            }
        }
    
        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", message);
    
            String jsonError = objectMapper.writeValueAsString(errorResponse);
            byte[] errorBytes = jsonError.getBytes(StandardCharsets.UTF_8);
    
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }

    static class RedrawHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
    
            try {
                // Check if game is initialized
                if (gameBoard == null) {
                    String errorResponse = "{\"error\": \"Game not initialized\"}";
                    byte[] errorBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, errorBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                    }
                    return;
                }
    
                // Perform the redraw operation
                Map<String, Object> result = gameBoard.redrawCurrentPlayerHand();
                
                // Convert response to JSON and send
                String response = objectMapper.writeValueAsString(result);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
    
            } catch (Exception e) {
                // Handle errors
                String errorResponse = "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
                byte[] errorBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }

        private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", message);
    
            String jsonError = objectMapper.writeValueAsString(errorResponse);
            byte[] errorBytes = jsonError.getBytes(StandardCharsets.UTF_8);
    
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }
    
}


