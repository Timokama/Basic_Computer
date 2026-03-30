package com.basiccomputer.handler;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.Map;

public class LogoutHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public LogoutHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Get session cookie
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("session=")) {
                    String sessionId = cookie.trim().substring(7);
                    sessions.remove(sessionId);
                    break;
                }
            }
        }
        
        // Clear cookie and redirect to home
        exchange.getResponseHeaders().set("Set-Cookie", "session=; Path=/; HttpOnly; Max-Age=0");
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(302, -1);
    }
}