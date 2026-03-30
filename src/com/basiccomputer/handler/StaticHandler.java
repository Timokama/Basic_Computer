package com.basiccomputer.handler;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;

public class StaticHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Determine content type
        String contentType = "text/html";
        if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (path.endsWith(".gif")) {
            contentType = "image/gif";
        }
        
        File file = null;
        
        // Check if it's a template request or CSS/JS
        if (path.startsWith("/templates/") || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/static/")) {
            String templatePath = path.substring(1); // Remove leading slash
            // Handle /static/css/ to templates/css/ mapping
            if (path.startsWith("/static/css/")) {
                templatePath = "templates/css/" + path.substring(12);
            } else if (path.startsWith("/static/js/")) {
                templatePath = "templates/js/" + path.substring(11);
            } else if (path.startsWith("/css/")) {
                templatePath = "templates/css/" + path.substring(5);
            } else if (path.startsWith("/js/")) {
                templatePath = "templates/js/" + path.substring(4);
            } else {
                templatePath = path.substring(1); // Remove leading slash for /templates/
            }

            // Try multiple possible locations
            File[] possiblePaths = {
                new File(templatePath),
                new File("basic-computer/" + templatePath),
                new File("../" + templatePath)
            };

            file = null;
            for (File f : possiblePaths) {
                if (f.exists() && !f.isDirectory()) {
                    file = f;
                    break;
                }
            }

            if (file != null && file.exists()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, fileContent.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileContent);
                os.close();
                return;
            }
        }
        
        // Try to serve from web directory
        String filePath = path.replace("/static/", "");
        file = new File("basic-computer/web/" + filePath);
        if (!file.exists()) {
            // Try alternate path
            file = new File("web/" + filePath);
        }
        
        if (file.exists() && !file.isDirectory()) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileContent.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileContent);
            os.close();
        } else {
            String response = "File not found: " + filePath;
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
