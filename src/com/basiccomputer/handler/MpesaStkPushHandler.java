package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MpesaStkPushHandler implements HttpHandler {
    // M-Pesa Configuration - Replace with your actual credentials
    private static final String CONSUMER_KEY = "YOUR_CONSUMER_KEY";
    private static final String CONSUMER_SECRET = "YOUR_CONSUMER_SECRET";
    private static final String SHORTCODE = "YOUR_SHORTCODE";
    private static final String PASSKEY = "YOUR_PASSKEY";
    private static final String AUTH_URL = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_URL = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            // Parse query parameters
            String query = exchange.getRequestURI().getQuery();
            String phone = getQueryParam(query, "phone");
            String amount = getQueryParam(query, "amount");
            String plan = getQueryParam(query, "plan");
            
            if (phone == null || amount == null) {
                sendJsonResponse(exchange, false, "Missing phone or amount");
                return;
            }
            
            // Format phone number (ensure it starts with 254)
            if (phone.startsWith("0")) {
                phone = "254" + phone.substring(1);
            }
            
            // For demo purposes, simulate successful STK push
            // In production, you would call the actual M-Pesa API
            System.out.println("Initiating STK Push for phone: " + phone + ", amount: " + amount);
            
            // Simulate processing
            // In production: get access token, then call STK push
            // For demo: return success
            sendJsonResponse(exchange, true, "STK push initiated successfully. Check your phone.");
            
        } catch (Exception e) {
            System.err.println("Error initiating STK push: " + e.getMessage());
            sendJsonResponse(exchange, false, "Payment request failed: " + e.getMessage());
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, boolean success, String message) throws IOException {
        String json = "{\"success\": " + success + ", \"message\": \"" + message + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
    
    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        String[] params = query.split("&");
        for (String p : params) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return kv[1];
            }
        }
        return null;
    }
}