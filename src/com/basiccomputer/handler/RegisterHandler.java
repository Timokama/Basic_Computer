package com.basiccomputer.handler;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class RegisterHandler implements HttpHandler {
    private Map<String, String> users;
    
    public RegisterHandler(Map<String, String> users) {
        this.users = users;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("GET".equalsIgnoreCase(method)) {
            showRegisterForm(exchange, null);
        } else if ("POST".equalsIgnoreCase(method)) {
            handleRegister(exchange);
        }
    }
    
    private void showRegisterForm(HttpExchange exchange, String error) throws IOException {
        String errorDiv = "";
        if (error != null && !error.isEmpty()) {
            errorDiv = "<div class=\"error-box\">" + error + "</div>";
        }
        
        String html = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>Register - Basic Computer</title>" +
            "    <style>" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
            "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }" +
            "        .register-container { background: white; border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); padding: 50px; width: 100%; max-width: 500px; }" +
            "        .logo { text-align: center; margin-bottom: 30px; }" +
            "        .logo h1 { color: #667eea; font-size: 32px; margin-bottom: 10px; }" +
            "        .logo p { color: #666; }" +
            "        .form-group { margin-bottom: 20px; }" +
            "        .form-group label { display: block; margin-bottom: 8px; color: #555; font-weight: 500; font-size: 14px; }" +
            "        .password-wrapper { position: relative; }" +
            "        .password-wrapper input { padding-right: 45px; }" +
            "        .toggle-password { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); cursor: pointer; color: #999; font-size: 18px; user-select: none; background: none; border: none; padding: 0; }" +
            "        .toggle-password:hover { color: #667eea; }" +
            "        .form-group input { width: 100%; padding: 14px; border: 2px solid #e0e0e0; border-radius: 10px; font-size: 15px; transition: border-color 0.3s; }" +
            "        .form-group input:focus { outline: none; border-color: #667eea; }" +
            "        .btn-register { width: 100%; padding: 16px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 10px; font-size: 18px; font-weight: 600; cursor: pointer; transition: transform 0.3s; margin-top: 10px; }" +
            "        .btn-register:hover { transform: translateY(-2px); }" +
            "        .link-text { text-align: center; margin-top: 25px; color: #666; font-size: 14px; }" +
            "        .link-text a { color: #667eea; text-decoration: none; font-weight: 600; }" +
            "        .link-text a:hover { text-decoration: underline; }" +
            "        .error-box { background: #fee; color: #c00; padding: 15px; border-radius: 8px; margin-bottom: 20px; text-align: center; border: 1px solid #fcc; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"register-container\">" +
            "        <div class=\"logo\">" +
            "            <h1>Create Account</h1>" +
            "            <p>Join Basic Computer Learning Platform</p>" +
            "        </div>" +
            errorDiv +
            "        <form method=\"POST\" action=\"/register\">" +
            "            <div class=\"form-group\">" +
            "                <label>Full Name</label>" +
            "                <input type=\"text\" name=\"fullName\" required placeholder=\"Enter your full name\">" +
            "            </div>" +
            "            <div class=\"form-group\">" +
            "                <label>Email Address</label>" +
            "                <input type=\"email\" name=\"email\" required placeholder=\"Enter your email\">" +
            "            </div>" +
            "            <div class=\"form-group\">" +
            "                <label>Phone Number (for M-Pesa)</label>" +
            "                <input type=\"tel\" name=\"phone\" placeholder=\"e.g., 254712345678\">" +
            "            </div>" +
            "            <div class=\"form-group\">" +
            "                <label>Password</label>" +
            "                <div class=\"password-wrapper\">" +
            "                    <input type=\"password\" id=\"password\" name=\"password\" required placeholder=\"Create a password\">" +
            "                    <button type=\"button\" class=\"toggle-password\" onclick=\"togglePassword('password', this)\">" +
            "                        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z\"></path><circle cx=\"12\" cy=\"12\" r=\"3\"></circle></svg>" +
            "                    </button>" +
            "                </div>" +
            "            </div>" +
            "            <div class=\"form-group\">" +
            "                <label>Confirm Password</label>" +
            "                <div class=\"password-wrapper\">" +
            "                    <input type=\"password\" id=\"confirmPassword\" name=\"confirmPassword\" required placeholder=\"Confirm your password\">" +
            "                    <button type=\"button\" class=\"toggle-password\" onclick=\"togglePassword('confirmPassword', this)\">" +
            "                        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z\"></path><circle cx=\"12\" cy=\"12\" r=\"3\"></circle></svg>" +
            "                    </button>" +
            "                </div>" +
            "            </div>" +
            "            <button type=\"submit\" class=\"btn-register\">Register</button>" +
            "        </form>" +
            "        <p class=\"link-text\">Already have an account? <a href=\"/login\">Login here</a></p>" +
            "        <p class=\"link-text\"><a href=\"/\">Back to Home</a></p>" +
            "    </div>" +
            "    <script>" +
            "        function togglePassword(fieldId, btn) {" +
            "            var field = document.getElementById(fieldId);" +
            "            var svg = btn.querySelector('svg');" +
            "            if (field.type === 'password') {" +
            "                field.type = 'text';" +
            "                svg.innerHTML = '<path d=\"M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24\"></path><line x1=\"1\" y1=\"1\" x2=\"23\" y2=\"23\"></line>';" +
            "            } else {" +
            "                field.type = 'password';" +
            "                svg.innerHTML = '<path d=\"M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z\"></path><circle cx=\"12\" cy=\"12\" r=\"3\"></circle>';" +
            "            }" +
            "        }" +
            "    </script>" +
            "</body>" +
            "</html>";
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(html.getBytes());
        os.close();
    }
    
    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(body);
        
        String fullName = params.get("fullName");
        String email = params.get("email");
        String phone = params.get("phone");
        String password = params.get("password");
        String confirmPassword = params.get("confirmPassword");
        
        if (!password.equals(confirmPassword)) {
            showRegisterForm(exchange, "Passwords do not match");
            return;
        }
        
        if (password.length() < 6) {
            showRegisterForm(exchange, "Password must be at least 6 characters");
            return;
        }
        
        try {
            if (com.basiccomputer.db.Database.userExists(email)) {
                showRegisterForm(exchange, "Email already registered");
                return;
            }
            
            // Register as STUDENT by default
            String role = "STUDENT";
            
            com.basiccomputer.db.Database.registerUser(email, password, fullName, phone, role);
            
            // Show success - redirect to login
            exchange.getResponseHeaders().set("Location", "/login?registered=1");
            exchange.sendResponseHeaders(302, -1);
            
        } catch (Exception e) {
            showRegisterForm(exchange, "Registration failed. Please try again.");
        }
    }
    
    private Map<String, String> parseFormData(String body) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (Exception e) {}
            }
        }
        return params;
    }
}
