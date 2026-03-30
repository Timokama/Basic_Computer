package com.basiccomputer.handler;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class LoginHandler implements HttpHandler {
    private Map<String, String> users;
    private Map<String, String> sessions;
    
    public LoginHandler(Map<String, String> users, Map<String, String> sessions) {
        this.users = users;
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("GET".equalsIgnoreCase(method)) {
            showLoginForm(exchange, null);
        } else if ("POST".equalsIgnoreCase(method)) {
            handleLogin(exchange);
        }
    }
    
    private void showLoginForm(HttpExchange exchange, String error) throws IOException {
        String errorDiv = "";
        if (error != null && !error.isEmpty()) {
            errorDiv = "<div class=\"error-box\">" + error + "</div>";
        }
        
        String html = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>Login - Basic Computer</title>" +
            "    <link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">" +
            "    <style>" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
            "        body {" +
            "            font-family: 'Poppins', sans-serif;" +
            "            background: #0a0a1a;" +
            "            min-height: 100vh;" +
            "            display: flex;" +
            "            justify-content: center;" +
            "            align-items: center;" +
            "            padding: 20px;" +
            "            position: relative;" +
            "            overflow: hidden;" +
            "        }" +
            "        body::before {" +
            "            content: '';" +
            "            position: absolute;" +
            "            top: 0; left: 0; right: 0; bottom: 0;" +
            "            background:" +
            "                radial-gradient(ellipse at 30% 30%, rgba(99, 102, 241, 0.15) 0%, transparent 50%)," +
            "                radial-gradient(ellipse at 70% 70%, rgba(139, 92, 246, 0.15) 0%, transparent 50%);" +
            "            animation: pulse 8s ease-in-out infinite;" +
            "        }" +
            "        @keyframes pulse {" +
            "            0%, 100% { opacity: 1; }" +
            "            50% { opacity: 0.6; }" +
            "        }" +
            "        .login-container {" +
            "            background: rgba(255, 255, 255, 0.05);" +
            "            backdrop-filter: blur(20px);" +
            "            border: 1px solid rgba(255, 255, 255, 0.1);" +
            "            border-radius: 24px;" +
            "            box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);" +
            "            padding: 50px;" +
            "            width: 100%;" +
            "            max-width: 450px;" +
            "            position: relative;" +
            "            animation: slideUp 0.6s ease-out;" +
            "        }" +
            "        @keyframes slideUp {" +
            "            from { opacity: 0; transform: translateY(30px); }" +
            "            to { opacity: 1; transform: translateY(0); }" +
            "        }" +
            "        .logo { text-align: center; margin-bottom: 30px; }" +
            "        .logo-icon {" +
            "            font-size: 50px;" +
            "            display: inline-block;" +
            "            animation: bounce 2s ease-in-out infinite;" +
            "            margin-bottom: 15px;" +
            "        }" +
            "        @keyframes bounce {" +
            "            0%, 100% { transform: translateY(0); }" +
            "            50% { transform: translateY(-10px); }" +
            "        }" +
            "        .logo h1 {" +
            "            background: linear-gradient(135deg, #6366f1, #8b5cf6, #06b6d4);" +
            "            -webkit-background-clip: text;" +
            "            -webkit-text-fill-color: transparent;" +
            "            font-size: 28px;" +
            "            font-weight: 700;" +
            "            margin-bottom: 8px;" +
            "        }" +
            "        .logo p { color: rgba(255, 255, 255, 0.6); font-size: 14px; }" +
            "        .form-group { margin-bottom: 25px; position: relative; }" +
            "        .form-group label {" +
            "            display: block;" +
            "            margin-bottom: 10px;" +
            "            color: rgba(255, 255, 255, 0.7);" +
            "            font-weight: 500;" +
            "            font-size: 14px;" +
            "        }" +
            "        .password-wrapper { position: relative; }" +
            "        .password-wrapper input { padding-right: 45px; }" +
            "        .toggle-password { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); cursor: pointer; color: rgba(255,255,255,0.5); background: none; border: none; padding: 0; display: flex; align-items: center; }" +
            "        .toggle-password:hover { color: #6366f1; }" +
            "        .form-group input {" +
            "            width: 100%;" +
            "            padding: 16px 20px;" +
            "            background: rgba(255, 255, 255, 0.05);" +
            "            border: 2px solid rgba(255, 255, 255, 0.1);" +
            "            border-radius: 12px;" +
            "            font-size: 16px;" +
            "            color: #fff;" +
            "            transition: all 0.3s ease;" +
            "            font-family: 'Poppins', sans-serif;" +
            "        }" +
            "        .form-group input::placeholder { color: rgba(255, 255, 255, 0.4); }" +
            "        .form-group input:focus {" +
            "            outline: none;" +
            "            border-color: #6366f1;" +
            "            background: rgba(99, 102, 241, 0.1);" +
            "            box-shadow: 0 0 20px rgba(99, 102, 241, 0.2);" +
            "        }" +
            "        .btn-login {" +
            "            width: 100%;" +
            "            padding: 16px;" +
            "            background: linear-gradient(135deg, #6366f1, #8b5cf6);" +
            "            color: white;" +
            "            border: none;" +
            "            border-radius: 12px;" +
            "            font-size: 16px;" +
            "            font-weight: 600;" +
            "            cursor: pointer;" +
            "            transition: all 0.3s ease;" +
            "            margin-top: 10px;" +
            "            font-family: 'Poppins', sans-serif;" +
            "        }" +
            "        .btn-login:hover {" +
            "            transform: translateY(-3px);" +
            "            box-shadow: 0 10px 30px rgba(99, 102, 241, 0.4);" +
            "        }" +
            "        .link-text { text-align: center; margin-top: 25px; color: rgba(255, 255, 255, 0.6); font-size: 14px; }" +
            "        .link-text a {" +
            "            color: #06b6d4;" +
            "            text-decoration: none;" +
            "            font-weight: 600;" +
            "            transition: color 0.3s;" +
            "        }" +
            "        .link-text a:hover { color: #22d3ee; }" +
            "        .error-box {" +
            "            background: rgba(239, 68, 68, 0.1);" +
            "            color: #ef4444;" +
            "            padding: 15px;" +
            "            border-radius: 12px;" +
            "            margin-bottom: 20px;" +
            "            text-align: center;" +
            "            border: 1px solid rgba(239, 68, 68, 0.3);" +
            "            animation: shake 0.5s ease;" +
            "        }" +
            "        @keyframes shake {" +
            "            0%, 100% { transform: translateX(0); }" +
            "            25% { transform: translateX(-10px); }" +
            "            75% { transform: translateX(10px); }" +
            "        }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"login-container\">" +
            "        <div class=\"logo\">" +
            "            <div class=\"logo-icon\">💻</div>" +
            "            <h1>Basic Computer</h1>" +
            "            <p>Welcome back! Login to continue learning</p>" +
            "        </div>" +
            errorDiv +
            "        <form method=\"POST\" action=\"/login\">" +
            "            <div class=\"form-group\">" +
            "                <label>Email Address</label>" +
            "                <input type=\"email\" name=\"email\" required placeholder=\"Enter your email\">" +
            "            </div>" +
            "            <div class=\"form-group\">" +
            "                <label>Password</label>" +
            "                <div class=\"password-wrapper\">" +
            "                    <input type=\"password\" id=\"password\" name=\"password\" required placeholder=\"Enter your password\">" +
            "                    <button type=\"button\" class=\"toggle-password\" onclick=\"togglePassword()\">" +
            "                        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z\"></path><circle cx=\"12\" cy=\"12\" r=\"3\"></circle></svg>" +
            "                    </button>" +
            "                </div>" +
            "            </div>" +
            "            <button type=\"submit\" class=\"btn-login\">Login</button>" +
            "        </form>" +
            "        <p class=\"link-text\">Don't have an account? <a href=\"/register\">Register here</a></p>" +
            "        <p class=\"link-text\"><a href=\"/\">Back to Home</a></p>" +
            "    </div>" +
            "    <script>" +
            "        function togglePassword() {" +
            "            var field = document.getElementById('password');" +
            "            var btn = document.querySelector('.toggle-password svg');" +
            "            if (field.type === 'password') {" +
            "                field.type = 'text';" +
            "                btn.innerHTML = '<path d=\"M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24\"></path><line x1=\"1\" y1=\"1\" x2=\"23\" y2=\"23\"></line>';" +
            "            } else {" +
            "                field.type = 'password';" +
            "                btn.innerHTML = '<path d=\"M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z\"></path><circle cx=\"12\" cy=\"12\" r=\"3\"></circle>';" +
            "            }" +
            "        }" +
            "    </script>" +
            "</body>" +
            "</html>";
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String password = params.get("password");
        
        try {
            Map<String, Object> user = com.basiccomputer.db.Database.authenticateUser(email, password);
            
            if (user != null && (Boolean) user.get("active")) {
                String sessionId = java.util.UUID.randomUUID().toString();
                sessions.put(sessionId, (String) user.get("email"));
                
                // Check user role - redirect to appropriate dashboard
                String userRole = (String) user.get("role");
                String redirectUrl = "/dashboard"; // Default to student dashboard
                if ("ADMIN".equalsIgnoreCase(userRole)) {
                    redirectUrl = "/admin";
                }
                
                // Redirect directly to dashboard without toast
                exchange.getResponseHeaders().set("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
                exchange.getResponseHeaders().set("Location", redirectUrl);
                exchange.sendResponseHeaders(302, -1);
                exchange.getResponseBody().close();
                return;
            } else {
                showLoginForm(exchange, "Invalid email or password. Please try again.");
            }
        } catch (Exception e) {
            showLoginForm(exchange, "Login failed. Please try again.");
        }
    }
    
    private Map<String, String> parseFormData(String body) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception e) {}
            }
        }
        return params;
    }
}
