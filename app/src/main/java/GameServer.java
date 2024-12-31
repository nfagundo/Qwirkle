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
            server.createContext("/api/init", new InitHandler());
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
            // Check if game is initialized
            if (gameBoard == null) {
                String errorResponse = "{\"error\": \"Game not initialized\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, errorResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            try {
                // Parse the move data from the request
                ObjectMapper mapper = new ObjectMapper();
                JsonNode moveData = mapper.readTree(exchange.getRequestBody());
                
                // Make the move using the shared gameBoard instance
                Map<String, Object> result = gameBoard.makeMove(moveData);
                
                // Send response
                String response = mapper.writeValueAsString(result);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                // Handle errors
                String errorResponse = "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, errorResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
    static class QwirkleController implements HttpHandler {
        private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON processing
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
                return;
            }
    
            try {
                // Read the request body
                InputStream requestBody = exchange.getRequestBody();
                List<Piece> moves = objectMapper.readValue(
                    requestBody, 
                    new TypeReference<List<Piece>>() {}
                );
    
                // Calculate score
                int score = gameBoard.calculateMoveScore(moves);
    
                // Prepare response
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("score", score);
                response.put("currentPlayer", gameBoard.getTurn());
    
                // Convert response to JSON
                String jsonResponse = objectMapper.writeValueAsString(response);
    
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
    
            } catch (Exception e) {
                // Handle errors
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
    
                String jsonError = objectMapper.writeValueAsString(errorResponse);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, jsonError.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonError.getBytes());
                }
            }
        }    
    }

    static class InitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // Check if game is initialized
                    if (gameBoard == null) {
                        String errorResponse = "{\"error\": \"Game not initialized\"}";
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, errorResponse.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(errorResponse.getBytes());
                        }
                        return;
                    }
    
                    // Get the current player's hand from the game board
                    List<Map<String,String>> playerHand = gameBoard.getCurrentHand();
                    
                    // Convert the hand to JSON
                    Map<String, Object> response = new HashMap<>();
                    response.put("hand", playerHand);
                    
                    String gameState = objectMapper.writeValueAsString(response);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, gameState.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(gameState.getBytes());
                    }
                } catch (Exception e) {
                    String errorResponse = "{\"error\": \"" + e.getMessage() + "\"}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, errorResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorResponse.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
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
                response.put("initialHand", gameBoard.getCurrentHand()); // Get first player's hand
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
}

