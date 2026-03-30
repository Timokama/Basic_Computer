package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class ContentViewHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public ContentViewHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String userEmail = getUserFromSession(exchange);
        
        if (userEmail == null) {
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        // Parse content ID from path
        String path = exchange.getRequestURI().getPath();
        String idStr = path.replace("/view/", "");
        int contentId = 0;
        try {
            contentId = Integer.parseInt(idStr);
        } catch (Exception e) {
            exchange.getResponseHeaders().set("Location", "/content");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        Map<String, Object> content = null;
        try {
            content = Database.getContentById(contentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (content == null) {
            exchange.getResponseHeaders().set("Location", "/content");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        String type = (String) content.get("type");
        String title = (String) content.get("title");
        String body = (String) content.get("body");
        String category = (String) content.get("category");
        
        // Determine icon and type label
        String icon = "Notes";
        String typeLabel = "Note";
        if ("VIDEO".equals(type)) { icon = "Video"; typeLabel = "Video"; }
        else if ("SAMPLE_QUESTION".equals(type)) { icon = "Question"; typeLabel = "Question"; }
        else if ("EXAM".equals(type)) { icon = "Exam"; typeLabel = "Exam"; }
        
        String html = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>" + title + " - Basic Computer</title>" +
            "    <style>" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
            "        body { font-family: 'Segoe UI', sans-serif; background: #f5f7fa; }" +
            "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px 40px; display: flex; justify-content: space-between; align-items: center; }" +
            "        .nav a { color: white; text-decoration: none; margin-left: 20px; padding: 10px 20px; border-radius: 5px; }" +
            "        .container { padding: 40px; max-width: 900px; margin: 0 auto; }" +
            "        .back-link { display: inline-block; margin-bottom: 20px; color: #667eea; text-decoration: none; }" +
            "        .back-link:hover { text-decoration: underline; }" +
            "        .content-box { background: white; padding: 40px; border-radius: 15px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            "        .type-badge { display: inline-block; background: #667eea; color: white; padding: 5px 15px; border-radius: 20px; font-size: 12px; margin-bottom: 15px; }" +
            "        .category-badge { display: inline-block; background: #764ba2; color: white; padding: 5px 15px; border-radius: 20px; font-size: 12px; margin-left: 10px; }" +
            "        h1 { color: #333; font-size: 28px; margin-bottom: 20px; }" +
            "        .content-body { color: #555; line-height: 1.8; font-size: 16px; white-space: pre-wrap; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"header\">" +
            "        <h1>Basic Computer</h1>" +
            "        <nav class=\"nav\">" +
            "            <a href=\"/dashboard\">Dashboard</a>" +
            "            <a href=\"/content\">All Content</a>" +
            "            <a href=\"/payment\">Subscribe</a>" +
            "            <a href=\"/logout\">Logout</a>" +
            "        </nav>" +
            "    </div>" +
            "    <div class=\"container\">" +
            "        <a href=\"/content\" class=\"back-link\">Back to Content</a>" +
            "        <div class=\"content-box\">" +
            "            <span class=\"type-badge\">" + typeLabel + "</span>" +
            "            <span class=\"category-badge\">" + category + "</span>" +
            "            <h1>" + title + "</h1>" +
            "            <div class=\"content-body\">" + body + "</div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(html.getBytes());
        os.close();
    }
    
    private String getUserFromSession(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("session=")) {
                    return sessions.get(cookie.trim().substring(7));
                }
            }
        }
        return null;
    }
}