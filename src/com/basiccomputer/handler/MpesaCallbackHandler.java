package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class MpesaCallbackHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("M-Pesa Callback received: " + body);
            
            // Parse the callback JSON
            // In production, you would parse the actual M-Pesa response
            // For demo purposes, we'll just acknowledge receipt
            
            String response = "{\"ResultCode\":0, \"ResultDesc\":\"Success\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            
        } catch (Exception e) {
            System.err.println("Error processing M-Pesa callback: " + e.getMessage());
            String response = "{\"ResultCode\":1, \"ResultDesc\":\"Failed\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}