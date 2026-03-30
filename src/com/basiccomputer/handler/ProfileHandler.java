package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.basiccomputer.util.TemplateUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ProfileHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public ProfileHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String userEmail = getUserFromSession(exchange);
        
        if ("POST".equals(method)) {
            if (userEmail == null) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            handlePost(exchange, userEmail);
            return;
        }
        
        handleGet(exchange, userEmail);
    }
    
    private void handleGet(HttpExchange exchange, String userEmail) throws IOException {
        String html = TemplateUtil.loadTemplate("profile");
        
        if (userEmail != null) {
            html = buildLoggedInProfile(html, userEmail);
        } else {
            html = buildPublicProfile(html);
        }
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private void handlePost(HttpExchange exchange, String userEmail) throws IOException {
        String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(formData);
        String action = params.get("action");
        
        String message = "";
        String messageType = "";
        
        Map<String, Object> user = null;
        try {
            user = Database.getUserByEmail(userEmail);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (user == null) {
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        int userId = (Integer) user.get("id");
        
        if ("update_profile".equals(action)) {
            String fullName = params.get("full_name");
            String phone = params.get("phone");
            if (fullName != null && !fullName.isEmpty()) {
                try {
                    Database.updateUserProfile(userId, fullName, phone);
                    message = "Profile updated successfully!";
                    messageType = "success";
                } catch (Exception e) {
                    e.printStackTrace();
                    message = "Failed to update profile.";
                    messageType = "error";
                }
            }
        } else if ("change_password".equals(action)) {
            String newPassword = params.get("new_password");
            String confirmPassword = params.get("confirm_password");
            if (newPassword != null && newPassword.equals(confirmPassword) && newPassword.length() >= 6) {
                try {
                    String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
                    Database.updateUserPassword(userId, hashedPassword);
                    message = "Password changed successfully!";
                    messageType = "success";
                } catch (Exception e) {
                    e.printStackTrace();
                    message = "Failed to change password.";
                    messageType = "error";
                }
            } else {
                message = "Passwords do not match or are too short (min 6 characters).";
                messageType = "error";
            }
        }
        
        // Re-render with message
        String html = TemplateUtil.loadTemplate("profile");
        html = buildLoggedInProfile(html, userEmail, message, messageType);
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private String buildLoggedInProfile(String html, String userEmail) {
        return buildLoggedInProfile(html, userEmail, "", "");
    }
    
    private String buildLoggedInProfile(String html, String userEmail, String message, String messageType) {
        Map<String, Object> user = null;
        Map<String, Object> subscription = null;
        try {
            user = Database.getUserByEmail(userEmail);
            if (user != null) {
                int userId = (Integer) user.get("id");
                subscription = Database.getUserSubscription(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String fullName = user != null ? (String) user.get("full_name") : "User";
        String email = user != null ? (String) user.get("email") : userEmail;
        String phone = user != null ? (String) user.get("phone_number") : "";
        String createdAt = user != null && user.get("created_at") != null ? user.get("created_at").toString() : "N/A";
        String userInitial = fullName.substring(0, 1).toUpperCase();
        
        String subscriptionStatus = "FREE";
        String expiryDate = "N/A";
        if (subscription != null) {
            subscriptionStatus = (String) subscription.get("type");
            if (subscription.get("end_date") != null) {
                expiryDate = subscription.get("end_date").toString();
            }
        }
        
        String createdAtShort = createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt;
        String expiryShort = expiryDate.length() > 10 ? expiryDate.substring(0, 10) : expiryDate;
        
        // Navigation for logged-in user
        html = html.replace("{{NAV_USER}}", "<div class=\"user-menu\"><div class=\"user-avatar\">" + userInitial + "</div><span>" + fullName.split(" ")[0] + "</span></div>");
        html = html.replace("{{NAV_AUTH}}", "<a href=\"/logout\">Logout</a>");
        
        // Message display
        String messageHtml = "";
        if (message != null && !message.isEmpty()) {
            messageHtml = "<div class=\"msg-" + messageType + "\">" + message + "</div>";
        }
        
        String subBadgeClass = "FREE".equals(subscriptionStatus) ? "sub-free" : "sub-active";
        String subLabel = "FREE".equals(subscriptionStatus) ? "Free Plan" : subscriptionStatus + " Plan";
        
        String content = messageHtml +
            "<div class=\"profile-hero\">" +
            "    <div class=\"profile-avatar\">" + userInitial + "</div>" +
            "    <h1>" + fullName + "</h1>" +
            "    <p>" + email + "</p>" +
            "    <span class=\"sub-badge " + subBadgeClass + "\">" + subLabel + "</span>" +
            "</div>" +
            
            "<div class=\"profile-grid\">" +
            "    <div class=\"profile-card\">" +
            "        <h3>📋 Account Information</h3>" +
            "        <div class=\"info-item\"><span class=\"info-label\">Full Name</span><span class=\"info-value\">" + fullName + "</span></div>" +
            "        <div class=\"info-item\"><span class=\"info-label\">Email</span><span class=\"info-value\">" + email + "</span></div>" +
            "        <div class=\"info-item\"><span class=\"info-label\">Phone</span><span class=\"info-value\">" + (phone != null && !phone.isEmpty() ? phone : "Not set") + "</span></div>" +
            "        <div class=\"info-item\"><span class=\"info-label\">Member Since</span><span class=\"info-value\">" + createdAtShort + "</span></div>" +
            "        <div class=\"info-item\"><span class=\"info-label\">Subscription Expires</span><span class=\"info-value\">" + expiryShort + "</span></div>" +
            "    </div>" +
            
            "    <div class=\"profile-card\">" +
            "        <h3>✏️ Update Profile</h3>" +
            "        <form method=\"POST\">" +
            "            <input type=\"hidden\" name=\"action\" value=\"update_profile\">" +
            "            <div class=\"form-group\"><label>Full Name</label><input class=\"form-input\" type=\"text\" name=\"full_name\" value=\"" + fullName + "\" required></div>" +
            "            <div class=\"form-group\"><label>Phone Number</label><input class=\"form-input\" type=\"tel\" name=\"phone\" value=\"" + (phone != null ? phone : "") + "\" placeholder=\"+2547XXXXXXXX\"></div>" +
            "            <button type=\"submit\" class=\"btn-primary\">Update Profile</button>" +
            "        </form>" +
            "    </div>" +
            
            "    <div class=\"profile-card\">" +
            "        <h3>🔐 Change Password</h3>" +
            "        <form method=\"POST\">" +
            "            <input type=\"hidden\" name=\"action\" value=\"change_password\">" +
            "            <div class=\"form-group\"><label>New Password</label><input class=\"form-input\" type=\"password\" name=\"new_password\" placeholder=\"Min 6 characters\" required minlength=\"6\"></div>" +
            "            <div class=\"form-group\"><label>Confirm Password</label><input class=\"form-input\" type=\"password\" name=\"confirm_password\" placeholder=\"Confirm password\" required minlength=\"6\"></div>" +
            "            <button type=\"submit\" class=\"btn-primary\">Change Password</button>" +
            "        </form>" +
            "    </div>" +
            
            "    <div class=\"profile-card\">" +
            "        <h3>🔗 Quick Links</h3>" +
            "        <a href=\"/content?type=NOTE\" class=\"quick-link\"><div class=\"quick-link-icon\">📚</div><div class=\"quick-link-text\"><h4>Study Notes</h4><p>Browse comprehensive CS notes</p></div></a>" +
            "        <a href=\"/content?type=SAMPLE_QUESTION\" class=\"quick-link\"><div class=\"quick-link-icon\">❓</div><div class=\"quick-link-text\"><h4>Practice Questions</h4><p>Test your knowledge</p></div></a>" +
            "        <a href=\"/content?type=EXAM\" class=\"quick-link\"><div class=\"quick-link-icon\">📝</div><div class=\"quick-link-text\"><h4>Exams</h4><p>Practice exams with answers</p></div></a>" +
            "        <a href=\"/content?type=VIDEO\" class=\"quick-link\"><div class=\"quick-link-icon\">🎥</div><div class=\"quick-link-text\"><h4>Video Tutorials</h4><p>Learn visually</p></div></a>" +
            "    </div>" +
            "</div>";
        
        html = html.replace("{{PROFILE_CONTENT}}", content);
        return html;
    }
    
    private String buildPublicProfile(String html) {
        html = html.replace("{{NAV_USER}}", "");
        html = html.replace("{{NAV_AUTH}}", "<a href=\"/login\">Login</a><a href=\"/register\">Register</a>");
        
        String content =
            "<div class=\"profile-hero\">" +
            "    <div class=\"profile-avatar\">👤</div>" +
            "    <h1>My Profile</h1>" +
            "    <p>Sign in to manage your account and track your progress</p>" +
            "</div>" +
            
            "<div class=\"profile-card\" style=\"max-width: 500px; margin: 0 auto; text-align: center;\">" +
            "    <div class=\"login-prompt\">" +
            "        <h2>Welcome to Basic Computer</h2>" +
            "        <p>Login or create an account to access your profile, track your learning progress, and manage your subscription.</p>" +
            "        <a href=\"/login\" class=\"login-btn\">🔑 Login</a>" +
            "        <div style=\"height: 12px\"></div>" +
            "        <a href=\"/register\" class=\"login-btn\" style=\"background: linear-gradient(135deg, #10b981, #059669);\">📝 Create Account</a>" +
            "    </div>" +
            "</div>";
        
        html = html.replace("{{PROFILE_CONTENT}}", content);
        return html;
    }
    
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
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
