package com.basiccomputer;

import com.basiccomputer.db.Database;
import com.basiccomputer.handler.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final int PORT = 8082;
    private static Map<String, String> users = new HashMap<>();
    private static Map<String, String> sessions = new HashMap<>();
    
    public static void main(String[] args) {
        try {
            // Initialize database
            Database.initialize();
            Database.insertSampleContent();
            Database.insertSampleProjects();
            System.out.println("Database initialized successfully");
            
            // Create HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
            
            // Create context handlers
            server.createContext("/", new RootHandler());
            server.createContext("/static", new StaticHandler());
            server.createContext("/login", new LoginHandler(users, sessions));
            server.createContext("/register", new RegisterHandler(users));
            server.createContext("/logout", new LogoutHandler(sessions));
            server.createContext("/dashboard", new DashboardHandler(sessions));
            server.createContext("/admin", new AdminDashboardHandler(sessions));
            server.createContext("/content", new ContentHandler(sessions));
            server.createContext("/view", new ContentViewHandler(sessions));
            server.createContext("/payment", new PaymentHandler(sessions));
            server.createContext("/profile", new ProfileHandler(sessions));
            server.createContext("/api/mpesa/callback", new MpesaCallbackHandler());
            server.createContext("/api/mpesa/stkpush", new MpesaStkPushHandler());
            server.createContext("/api/projects", new ProjectHandler(sessions));
            server.createContext("/api/admin", new AdminApiHandler(sessions));
            
            // Set executor
            server.setExecutor(null);
            
            server.start();
            
            // Get local and external IP addresses
            String localIP = "localhost";
            String externalIP = "<use port forwarding or ngrok>";
            try {
                java.net.InetAddress ip = java.net.InetAddress.getLocalHost();
                java.net.NetworkInterface network = java.net.NetworkInterface.getByInetAddress(ip);
                if (network != null) {
                    java.util.Enumeration<java.net.InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                            localIP = addr.getHostAddress();
                            break;
                        }
                    }
                }
                // Try to get external IP
                try {
                    java.net.URL url = new java.net.URL("https://api.ipify.org");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(2000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    externalIP = reader.readLine();
                    reader.close();
                } catch (Exception e) {
                    // Could not get external IP
                }
            } catch (Exception e) {
                localIP = "localhost";
            }
            
            System.out.println("========================================");
            System.out.println("Server running at:");
            System.out.println("  Local:     http://localhost:" + PORT);
            System.out.println("  Network:   http://" + localIP + ":" + PORT);
            System.out.println("  External:  http://" + externalIP + ":" + PORT);
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static Map<String, String> getUsers() {
        return users;
    }
    
    public static Map<String, String> getSessions() {
        return sessions;
    }
}