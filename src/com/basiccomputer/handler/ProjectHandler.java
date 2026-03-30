package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ProjectHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public ProjectHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        // Set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        // Handle /api/projects paths
        if (path.equals("/api/projects") || path.equals("/api/projects/")) {
            if ("GET".equals(method)) {
                handleGetAll(exchange);
            } else if ("POST".equals(method)) {
                handleCreate(exchange);
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
            return;
        }
        
        // Handle /api/projects/{id} paths
        if (path.startsWith("/api/projects/")) {
            String idStr = path.substring("/api/projects/".length());
            // Remove trailing slash if present
            if (idStr.endsWith("/")) {
                idStr = idStr.substring(0, idStr.length() - 1);
            }
            try {
                int id = Integer.parseInt(idStr);
                if ("GET".equals(method)) {
                    handleGetById(exchange, id);
                } else if ("PUT".equals(method)) {
                    handleUpdate(exchange, id);
                } else if ("DELETE".equals(method)) {
                    handleDelete(exchange, id);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"Invalid project ID\"}");
            }
            return;
        }
        
        sendJson(exchange, 404, "{\"error\":\"Not found\"}");
    }
    
    private void handleGetAll(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, Object>> projects = Database.getAllProjects();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < projects.size(); i++) {
                if (i > 0) json.append(",");
                json.append(projectToJson(projects.get(i)));
            }
            json.append("]");
            sendJson(exchange, 200, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Failed to fetch projects\"}");
        }
    }
    
    private void handleGetById(HttpExchange exchange, int id) throws IOException {
        try {
            Map<String, Object> project = Database.getProjectById(id);
            if (project != null) {
                sendJson(exchange, 200, projectToJson(project));
            } else {
                sendJson(exchange, 404, "{\"error\":\"Project not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Failed to fetch project\"}");
        }
    }
    
    private void handleCreate(HttpExchange exchange) throws IOException {
        String userEmail = getUserFromSession(exchange);
        if (userEmail == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseJsonBody(body);
            
            String title = params.getOrDefault("title", "");
            String description = params.getOrDefault("description", "");
            String projectUrl = params.getOrDefault("project_url", "");
            String status = params.getOrDefault("status", "In Progress");
            int progress = Integer.parseInt(params.getOrDefault("progress", "0"));
            String technologies = params.getOrDefault("technologies", "");
            String coverColor = params.getOrDefault("cover_color", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");
            String icon = params.getOrDefault("icon", "\uD83D\uDCBB");
            
            if (title.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"Title is required\"}");
                return;
            }
            
            Map<String, Object> user = Database.getUserByEmail(userEmail);
            int userId = user != null ? (Integer) user.get("id") : 1;
            
            int projectId = Database.addProject(title, description, projectUrl, status, progress, technologies, coverColor, icon, userId);
            
            Map<String, Object> project = Database.getProjectById(projectId);
            sendJson(exchange, 201, projectToJson(project));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Failed to create project\"}");
        }
    }
    
    private void handleUpdate(HttpExchange exchange, int id) throws IOException {
        String userEmail = getUserFromSession(exchange);
        if (userEmail == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseJsonBody(body);
            
            Map<String, Object> existing = Database.getProjectById(id);
            if (existing == null) {
                sendJson(exchange, 404, "{\"error\":\"Project not found\"}");
                return;
            }
            
            String title = params.getOrDefault("title", (String) existing.get("title"));
            String description = params.getOrDefault("description", (String) existing.get("description"));
            String projectUrl = params.getOrDefault("project_url", (String) existing.get("project_url"));
            String status = params.getOrDefault("status", (String) existing.get("status"));
            int progress = Integer.parseInt(params.getOrDefault("progress", String.valueOf(existing.get("progress"))));
            String technologies = params.getOrDefault("technologies", (String) existing.get("technologies"));
            String coverColor = params.getOrDefault("cover_color", (String) existing.get("cover_color"));
            String icon = params.getOrDefault("icon", (String) existing.get("icon"));
            
            Database.updateProject(id, title, description, projectUrl, status, progress, technologies, coverColor, icon);
            
            Map<String, Object> project = Database.getProjectById(id);
            sendJson(exchange, 200, projectToJson(project));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Failed to update project\"}");
        }
    }
    
    private void handleDelete(HttpExchange exchange, int id) throws IOException {
        String userEmail = getUserFromSession(exchange);
        if (userEmail == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        
        try {
            Database.deleteProject(id);
            sendJson(exchange, 200, "{\"message\":\"Project deleted\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Failed to delete project\"}");
        }
    }
    
    private String projectToJson(Map<String, Object> project) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(project.get("id")).append(",");
        sb.append("\"title\":\"").append(escapeJson((String) project.get("title"))).append("\",");
        sb.append("\"description\":\"").append(escapeJson((String) project.get("description"))).append("\",");
        sb.append("\"project_url\":\"").append(escapeJson((String) project.get("project_url"))).append("\",");
        sb.append("\"status\":\"").append(escapeJson((String) project.get("status"))).append("\",");
        sb.append("\"progress\":").append(project.get("progress")).append(",");
        sb.append("\"technologies\":\"").append(escapeJson((String) project.get("technologies"))).append("\",");
        sb.append("\"cover_color\":\"").append(escapeJson((String) project.get("cover_color"))).append("\",");
        sb.append("\"icon\":\"").append(escapeJson((String) project.get("icon"))).append("\"");
        sb.append("}");
        return sb.toString();
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    private Map<String, String> parseJsonBody(String body) {
        Map<String, String> params = new HashMap<>();
        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
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
    
    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private String getUserFromSession(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("session=")) {
                    String sessionId = cookie.trim().substring(8);
                    return sessions.get(sessionId);
                }
            }
        }
        return null;
    }
}
