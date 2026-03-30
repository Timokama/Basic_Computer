package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class AdminApiHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public AdminApiHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String userEmail = getUserFromSession(exchange);
        
        if (userEmail == null) {
            sendJsonResponse(exchange, 401, false, "Not authenticated");
            return;
        }
        
        try {
            Map<String, Object> user = Database.getUserByEmail(userEmail);
            if (user == null || !"ADMIN".equalsIgnoreCase((String) user.get("role"))) {
                sendJsonResponse(exchange, 403, false, "Not authorized");
                return;
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Server error");
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/admin/users") && "POST".equalsIgnoreCase(method)) {
                handleCreateUser(exchange);
            } else if (path.equals("/api/admin/content") && "POST".equalsIgnoreCase(method)) {
                handleCreateContent(exchange);
            } else if (path.matches("/api/admin/users/\\d+") && "DELETE".equalsIgnoreCase(method)) {
                handleDeleteUser(exchange, path);
            } else if (path.matches("/api/admin/content/\\d+") && "DELETE".equalsIgnoreCase(method)) {
                handleDeleteContent(exchange, path);
            } else if (path.matches("/api/admin/users/\\d+") && "PUT".equalsIgnoreCase(method)) {
                handleUpdateUser(exchange, path);
            } else if (path.matches("/api/admin/content/\\d+") && "PUT".equalsIgnoreCase(method)) {
                handleUpdateContent(exchange, path);
            } else {
                sendJsonResponse(exchange, 404, false, "Not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, false, "Server error: " + e.getMessage());
        }
    }
    
    private void handleCreateUser(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseJsonBody(body);
        
        String email = params.get("email");
        String password = params.get("password");
        String fullName = params.get("fullName");
        String phone = params.get("phone");
        String role = params.get("role");
        
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            sendJsonResponse(exchange, 400, false, "Email and password are required");
            return;
        }
        
        if (role == null || role.isEmpty()) {
            role = "STUDENT";
        }
        
        try {
            if (Database.userExists(email)) {
                sendJsonResponse(exchange, 400, false, "Email already exists");
                return;
            }
            
            Database.registerUser(email, password, fullName != null ? fullName : "", phone != null ? phone : "", role);
            sendJsonResponse(exchange, 200, true, "User created successfully");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to create user: " + e.getMessage());
        }
    }
    
    private void handleCreateContent(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseJsonBody(body);
        
        String title = params.get("title");
        String description = params.get("description");
        String type = params.get("type");
        String contentUrl = params.get("contentUrl");
        String contentBody = params.get("body");
        String category = params.get("category");
        
        if (title == null || title.isEmpty()) {
            sendJsonResponse(exchange, 400, false, "Title is required");
            return;
        }
        
        if (type == null || type.isEmpty()) {
            type = "NOTE";
        }
        
        try {
            // Get admin user ID
            String userEmail = getUserFromSession(exchange);
            Map<String, Object> user = Database.getUserByEmail(userEmail);
            int userId = user != null && user.get("id") != null ? (Integer) user.get("id") : 0;
            
            Database.addContent(title, description != null ? description : "", type, 
                contentUrl != null ? contentUrl : "", contentBody != null ? contentBody : "", 
                category != null ? category : "", userId);
            sendJsonResponse(exchange, 200, true, "Content created successfully");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to create content: " + e.getMessage());
        }
    }
    
    private void handleDeleteUser(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring(path.lastIndexOf("/") + 1);
        try {
            int id = Integer.parseInt(idStr);
            Database.deleteUser(id);
            sendJsonResponse(exchange, 200, true, "User deleted successfully");
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, false, "Invalid user ID");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to delete user: " + e.getMessage());
        }
    }
    
    private void handleDeleteContent(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring(path.lastIndexOf("/") + 1);
        try {
            int id = Integer.parseInt(idStr);
            Database.deleteContent(id);
            sendJsonResponse(exchange, 200, true, "Content deleted successfully");
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, false, "Invalid content ID");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to delete content: " + e.getMessage());
        }
    }
    
    private void handleUpdateUser(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring(path.lastIndexOf("/") + 1);
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseJsonBody(body);
        
        try {
            int id = Integer.parseInt(idStr);
            String fullName = params.get("fullName");
            String role = params.get("role");
            String active = params.get("active");
            
            Database.updateUser(id, fullName, role, "true".equalsIgnoreCase(active));
            sendJsonResponse(exchange, 200, true, "User updated successfully");
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, false, "Invalid user ID");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to update user: " + e.getMessage());
        }
    }
    
    private void handleUpdateContent(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring(path.lastIndexOf("/") + 1);
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseJsonBody(body);
        
        try {
            int id = Integer.parseInt(idStr);
            String title = params.get("title");
            String type = params.get("type");
            String category = params.get("category");
            
            Database.updateContent(id, title, type, category);
            sendJsonResponse(exchange, 200, true, "Content updated successfully");
        } catch (NumberFormatException e) {
            sendJsonResponse(exchange, 400, false, "Invalid content ID");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, false, "Failed to update content: " + e.getMessage());
        }
    }
    
    private Map<String, String> parseJsonBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) return params;
        
        body = body.trim();
        if (body.startsWith("{")) {
            body = body.substring(1, body.length() - 1);
            String[] pairs = body.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    params.put(key, value);
                }
            }
        }
        return params;
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, boolean success, String message) throws IOException {
        String json = "{\"success\":" + success + ",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private String getUserFromSession(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] parts = cookie.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("session=")) {
                        String sessionId = part.substring(8);
                        return sessions.get(sessionId);
                    }
                }
            }
        }
        return null;
    }
}
