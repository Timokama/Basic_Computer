package com.basiccomputer.handler;

import com.basiccomputer.util.TemplateUtil;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import com.basiccomputer.db.Database;

public class AdminDashboardHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public AdminDashboardHandler(Map<String, String> sessions) {
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
        
        try {
            Map<String, Object> user = Database.getUserByEmail(userEmail);
            if (user == null || !"ADMIN".equalsIgnoreCase((String) user.get("role"))) {
                exchange.getResponseHeaders().set("Location", "/dashboard");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            
            // Load template and replace placeholders
            String html = TemplateUtil.loadTemplate("admin");
            
            String userName = userEmail.contains("@") ? userEmail.split("@")[0] : userEmail;
            String safeName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
            
            int totalUsers = 0;
            int totalContent = 0;
            int activeSubscriptions = 0;
            int activeUsersCount = 0;
            
            StringBuilder usersTable = new StringBuilder();
            StringBuilder contentTable = new StringBuilder();
            
            try {
                List<Map<String, Object>> allUsers = Database.getAllUsers();
                List<Map<String, Object>> allContent = Database.getAllContent();
                List<Map<String, Object>> subscriptions = Database.getAllSubscriptions();
                
                totalUsers = allUsers != null ? allUsers.size() : 0;
                totalContent = allContent != null ? allContent.size() : 0;
                activeSubscriptions = subscriptions != null ? subscriptions.size() : 0;
                
                if (allUsers != null) {
                    for (Map<String, Object> u : allUsers) {
                        if (u.get("active") != null && (Boolean) u.get("active")) {
                            activeUsersCount++;
                        }
                    }
                }
                
                // Build users table
                if (allUsers != null && !allUsers.isEmpty()) {
                    for (Map<String, Object> u : allUsers) {
                        String email = (String) u.get("email");
                        String fullName = (String) u.get("full_name");
                        String role = (String) u.get("role");
                        boolean active = u.get("active") != null ? (Boolean) u.get("active") : false;
                        String createdAt = u.get("created_at") != null ? u.get("created_at").toString() : "N/A";
                        
                        usersTable.append("<tr class='user-row' data-id='").append(u.get("id")).append("'");
                        usersTable.append(" data-email='").append(email != null ? email.replace("'", "\\'") : "");
                        usersTable.append("'");
                        usersTable.append(" data-full-name='").append(fullName != null ? fullName.replace("'", "\\'") : "");
                        usersTable.append("'");
                        usersTable.append(" data-role='").append(role != null ? role : "STUDENT").append("'");
                        usersTable.append(" data-active='").append(active).append("'");
                        usersTable.append(">");
                        usersTable.append("<td>").append(email != null ? email : "N/A").append("</td>");
                        usersTable.append("<td>").append(fullName != null ? fullName : "N/A").append("</td>");
                        usersTable.append("<td><span class='badge ");
                        if ("ADMIN".equals(role)) {
                            usersTable.append("badge-admin");
                        } else {
                            usersTable.append("badge-student");
                        }
                        usersTable.append("'>").append(role).append("</span></td>");
                        usersTable.append("<td>");
                        if (active) {
                            usersTable.append("<span class='status-active'>Active</span>");
                        } else {
                            usersTable.append("<span class='status-inactive'>Inactive</span>");
                        }
                        usersTable.append("</td>");
                        usersTable.append("<td>").append(createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt).append("</td>");
                        usersTable.append("<td>");
                        usersTable.append("<button class='btn-action btn-edit' onclick='editUser(").append(u.get("id")).append(")'>Edit</button>");
                        usersTable.append("<button class='btn-action btn-delete' onclick='deleteUser(").append(u.get("id")).append(")'>Delete</button>");
                        usersTable.append("</td>");
                        usersTable.append("</tr>");
                    }
                } else {
                    usersTable.append("<tr><td colspan='6' style='text-align:center;'>No users found</td></tr>");
                }
                
                // Build content table
                if (allContent != null && !allContent.isEmpty()) {
                    for (Map<String, Object> c : allContent) {
                        String cTitle = (String) c.get("title");
                        String cType = (String) c.get("content_type");
                        String cCategory = (String) c.get("category");
                        
                        contentTable.append("<tr class='content-row' data-id='").append(c.get("id")).append("'");
                        contentTable.append(" data-title='").append(cTitle != null ? cTitle.replace("'", "\\'") : "");
                        contentTable.append("'");
                        contentTable.append(" data-type='").append(cType != null ? cType : "NOTE").append("'");
                        contentTable.append(" data-category='").append(cCategory != null ? cCategory.replace("'", "\\'") : "").append("'");
                        contentTable.append(">");
                        contentTable.append("<td>").append(cTitle != null ? cTitle : "N/A").append("</td>");
                        contentTable.append("<td><span class='badge badge-");
                        if (cType != null) {
                            contentTable.append(cType.toLowerCase().replace("_", ""));
                        } else {
                            contentTable.append("note");
                        }
                        contentTable.append("'>").append(cType != null ? cType.replace("_", " ") : "NOTE").append("</span></td>");
                        contentTable.append("<td>").append(cCategory != null ? cCategory : "N/A").append("</td>");
                        contentTable.append("<td>");
                        contentTable.append("<button class='btn-action btn-edit' onclick='editContent(").append(c.get("id")).append(")'>Edit</button>");
                        contentTable.append("<button class='btn-action btn-delete' onclick='deleteContent(").append(c.get("id")).append(")'>Delete</button>");
                        contentTable.append("</td>");
                        contentTable.append("</tr>");
                    }
                } else {
                    contentTable.append("<tr><td colspan='4' style='text-align:center;'>No content found</td></tr>");
                }
            } catch (Exception e) {
                System.err.println("Error fetching admin data: " + e.getMessage());
                usersTable.append("<tr><td colspan='6' style='text-align:center;'>Error loading users</td></tr>");
                contentTable.append("<tr><td colspan='4' style='text-align:center;'>Error loading content</td></tr>");
            }
            
            // Replace placeholders
            html = html.replace("{{USER_NAME}}", safeName);
            html = html.replace("{{TOTAL_USERS}}", String.valueOf(totalUsers));
            html = html.replace("{{TOTAL_CONTENT}}", String.valueOf(totalContent));
            html = html.replace("{{TOTAL_SUBSCRIPTIONS}}", String.valueOf(activeSubscriptions));
            html = html.replace("{{ACTIVE_USERS}}", String.valueOf(activeUsersCount));
            html = html.replace("{{USERS_TABLE}}", usersTable.toString());
            html = html.replace("{{CONTENT_TABLE}}", contentTable.toString());
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = html.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
        }
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
