package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.basiccomputer.util.TemplateUtil;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class DashboardHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public DashboardHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String userEmail = getUserFromSession(exchange);
        
        if (userEmail == null) {
            String html = getSubscribePageHtml();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = html.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
            return;
        }
        
        // Get user's name from email
        String userName = userEmail.contains("@") ? userEmail.split("@")[0] : userEmail;
        String displayName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
        String userInitial = userName.substring(0, 1).toUpperCase();
        
        // Get subscription status
        String subscriptionStatus = "FREE";
        String subscriptionType = "";
        try {
            Map<String, Object> user = Database.getUserByEmail(userEmail);
            if (user != null && user.get("id") != null) {
                int userId = (Integer) user.get("id");
                Map<String, Object> subscription = Database.getUserSubscription(userId);
                if (subscription != null) {
                    subscriptionStatus = "ACTIVE";
                    subscriptionType = (String) subscription.get("type");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Load template and replace placeholders
        String html = TemplateUtil.loadTemplate("dashboard");
        html = html.replace("{{USER_NAME}}", displayName);
        html = html.replace("{{USER_INITIAL}}", userInitial);
        
        // Subscription badge
        if ("ACTIVE".equals(subscriptionStatus) && subscriptionType != null && !subscriptionType.isEmpty()) {
            html = html.replace("{{SUBSCRIPTION_BADGE}}", "<span class=\"sub-badge sub-active\">📦 " + subscriptionType + " Plan</span>");
        } else {
            html = html.replace("{{SUBSCRIPTION_BADGE}}", "<span class=\"sub-badge sub-free\">Free Plan</span>");
        }
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private String getSubscribePageHtml() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Subscribe - Basic Computer</title>\n" +
            "    <style>\n" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "        body {\n" +
            "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
            "            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);\n" +
            "            min-height: 100vh;\n" +
            "            padding: 40px 20px;\n" +
            "            color: #fff;\n" +
            "        }\n" +
            "        .navbar {\n" +
            "            background: rgba(255, 255, 255, 0.1);\n" +
            "            padding: 1rem 2rem;\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            backdrop-filter: blur(10px);\n" +
            "            border-radius: 15px;\n" +
            "            margin-bottom: 30px;\n" +
            "        }\n" +
            "        .navbar h1 {\n" +
            "            color: #00d9ff;\n" +
            "            font-size: 1.5rem;\n" +
            "        }\n" +
            "        .nav-links a {\n" +
            "            color: #fff;\n" +
            "            text-decoration: none;\n" +
            "            margin-left: 1.5rem;\n" +
            "            padding: 0.5rem 1rem;\n" +
            "            border-radius: 5px;\n" +
            "            transition: background 0.3s;\n" +
            "        }\n" +
            "        .nav-links a:hover {\n" +
            "            background: rgba(0, 217, 255, 0.2);\n" +
            "        }\n" +
            "        .container {\n" +
            "            max-width: 900px;\n" +
            "            margin: 0 auto;\n" +
            "            background: rgba(255, 255, 255, 0.05);\n" +
            "            border-radius: 20px;\n" +
            "            padding: 40px;\n" +
            "            backdrop-filter: blur(10px);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.1);\n" +
            "        }\n" +
            "        h1 { color: #00d9ff; text-align: center; margin-bottom: 10px; }\n" +
            "        .subtitle { text-align: center; color: #aaa; margin-bottom: 30px; }\n" +
            "        \n" +
            "        /* Plans */\n" +
            "        .plans-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(3, 1fr);\n" +
            "            gap: 20px;\n" +
            "            margin-bottom: 30px;\n" +
            "        }\n" +
            "        @media (max-width: 768px) {\n" +
            "            .plans-grid { grid-template-columns: 1fr; }\n" +
            "        }\n" +
            "        .plan {\n" +
            "            border: 2px solid rgba(255, 255, 255, 0.1);\n" +
            "            border-radius: 15px;\n" +
            "            padding: 20px;\n" +
            "            cursor: pointer;\n" +
            "            transition: all 0.3s;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        .plan:hover, .plan.selected {\n" +
            "            border-color: #00d9ff;\n" +
            "            background: rgba(0, 217, 255, 0.1);\n" +
            "        }\n" +
            "        .plan-name { font-size: 18px; font-weight: bold; color: #fff; margin-bottom: 10px; }\n" +
            "        .plan-price { font-size: 28px; color: #00d9ff; font-weight: bold; margin-bottom: 15px; }\n" +
            "        .plan-features {\n" +
            "            text-align: left;\n" +
            "            padding-left: 10px;\n" +
            "            color: #aaa;\n" +
            "            font-size: 13px;\n" +
            "        }\n" +
            "        .plan-features li { margin-bottom: 8px; }\n" +
            "        \n" +
            "        /* Payment Methods Tabs */\n" +
            "        .payment-tabs {\n" +
            "            display: flex;\n" +
            "            gap: 10px;\n" +
            "            margin-bottom: 20px;\n" +
            "            border-bottom: 2px solid rgba(255, 255, 255, 0.1);\n" +
            "            padding-bottom: 10px;\n" +
            "        }\n" +
            "        .payment-tab {\n" +
            "            padding: 12px 24px;\n" +
            "            border: none;\n" +
            "            border-radius: 10px;\n" +
            "            background: rgba(255, 255, 255, 0.1);\n" +
            "            color: #fff;\n" +
            "            cursor: pointer;\n" +
            "            font-size: 14px;\n" +
            "            transition: all 0.3s;\n" +
            "        }\n" +
            "        .payment-tab:hover, .payment-tab.active {\n" +
            "            background: #00d9ff;\n" +
            "            color: #1a1a2e;\n" +
            "        }\n" +
            "        \n" +
            "        /* Payment Forms */\n" +
            "        .payment-form {\n" +
            "            display: none;\n" +
            "            padding: 30px;\n" +
            "            background: rgba(255, 255, 255, 0.05);\n" +
            "            border-radius: 15px;\n" +
            "        }\n" +
            "        .payment-form.active {\n" +
            "            display: block;\n" +
            "        }\n" +
            "        .form-group { margin-bottom: 20px; }\n" +
            "        label { display: block; margin-bottom: 8px; color: #aaa; font-weight: 500; }\n" +
            "        input {\n" +
            "            width: 100%;\n" +
            "            padding: 12px;\n" +
            "            border: 2px solid rgba(255, 255, 255, 0.2);\n" +
            "            border-radius: 10px;\n" +
            "            background: rgba(255, 255, 255, 0.1);\n" +
            "            color: #fff;\n" +
            "            font-size: 14px;\n" +
            "        }\n" +
            "        input:focus { outline: none; border-color: #00d9ff; }\n" +
            "        input::placeholder { color: #666; }\n" +
            "        .btn {\n" +
            "            width: 100%;\n" +
            "            padding: 15px;\n" +
            "            background: linear-gradient(135deg, #00d9ff 0%, #00a8cc 100%);\n" +
            "            color: #1a1a2e;\n" +
            "            border: none;\n" +
            "            border-radius: 10px;\n" +
            "            font-size: 16px;\n" +
            "            font-weight: bold;\n" +
            "            cursor: pointer;\n" +
            "            transition: all 0.3s;\n" +
            "        }\n" +
            "        .btn:hover { transform: translateY(-2px); box-shadow: 0 5px 20px rgba(0, 217, 255, 0.4); }\n" +
            "        .btn:disabled { background: #555; cursor: not-allowed; }\n" +
            "        .note {\n" +
            "            text-align: center;\n" +
            "            color: #666;\n" +
            "            font-size: 12px;\n" +
            "            margin-top: 15px;\n" +
            "        }\n" +
            "        \n" +
            "        /* M-Pesa */\n" +
            "        .mpesa-info {\n" +
            "            background: rgba(0, 100, 0, 0.2);\n" +
            "            border: 1px solid #00ff88;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 10px;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        .mpesa-info h3 { color: #00ff88; margin-bottom: 10px; }\n" +
            "        \n" +
            "        /* PayPal */\n" +
            "        .paypal-info {\n" +
            "            background: rgba(0, 100, 200, 0.2);\n" +
            "            border: 1px solid #00bfff;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 10px;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        .paypal-info h3 { color: #00bfff; margin-bottom: 10px; }\n" +
            "        \n" +
            "        /* Stripe */\n" +
            "        .stripe-info {\n" +
            "            background: rgba(100, 50, 150, 0.2);\n" +
            "            border: 1px solid #a855f7;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 10px;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        .stripe-info h3 { color: #a855f7; margin-bottom: 10px; }\n" +
            "        \n" +
            "        /* Bank Transfer */\n" +
            "        .bank-info {\n" +
            "            background: rgba(150, 100, 50, 0.2);\n" +
            "            border: 1px solid #f59e0b;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 10px;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        .bank-info h3 { color: #f59e0b; margin-bottom: 10px; }\n" +
            "        .bank-details {\n" +
            "            background: rgba(0, 0, 0, 0.3);\n" +
            "            padding: 15px;\n" +
            "            border-radius: 8px;\n" +
            "            margin-top: 15px;\n" +
            "        }\n" +
            "        .bank-details p { margin-bottom: 8px; color: #ccc; font-size: 14px; }\n" +
            "        .bank-details strong { color: #f59e0b; }\n" +
            "        \n" +
            "        .success-msg {\n" +
            "            padding: 15px;\n" +
            "            background: rgba(0, 255, 136, 0.2);\n" +
            "            border: 1px solid #00ff88;\n" +
            "            border-radius: 10px;\n" +
            "            color: #00ff88;\n" +
            "            text-align: center;\n" +
            "            margin-bottom: 20px;\n" +
            "            display: none;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <nav class=\"navbar\">\n" +
            "        <h1>💻 Basic Computer</h1>\n" +
            "        <div class=\"nav-links\">\n" +
            "            <a href=\"/login\">Login</a>\n" +
            "            <a href=\"/register\">Register</a>\n" +
            "        </div>\n" +
            "    </nav>\n" +
            "\n" +
            "    <div class=\"container\">\n" +
            "        <h1>💳 Subscribe</h1>\n" +
            "        <p class=\"subtitle\">Choose a plan and payment method to get started</p>\n" +
            "        \n" +
            "        <!-- Plans -->\n" +
            "        <div class=\"plans-grid\">\n" +
            "            <div class=\"plan selected\" onclick=\"selectPlan('MONTHLY', 500)\">\n" +
            "                <div class=\"plan-name\">Monthly Plan</div>\n" +
            "                <div class=\"plan-price\">KSh 500<span style=\"font-size:14px\">/mo</span></div>\n" +
            "                <ul class=\"plan-features\">\n" +
            "                    <li>✓ All study materials</li>\n" +
            "                    <li>✓ Video tutorials</li>\n" +
            "                    <li>✓ Practice questions</li>\n" +
            "                    <li>✓ Exams & answers</li>\n" +
            "                    <li>✓ 30 days access</li>\n" +
            "                </ul>\n" +
            "            </div>\n" +
            "            <div class=\"plan\" onclick=\"selectPlan('QUARTERLY', 1200)\">\n" +
            "                <div class=\"plan-name\">Quarterly Plan</div>\n" +
            "                <div class=\"plan-price\">KSh 1,200<span style=\"font-size:14px\">/3mo</span></div>\n" +
            "                <ul class=\"plan-features\">\n" +
            "                    <li>✓ All monthly features</li>\n" +
            "                    <li>✓ 3 months access</li>\n" +
            "                    <li>✓ Save KSh 300</li>\n" +
            "                </ul>\n" +
            "            </div>\n" +
            "            <div class=\"plan\" onclick=\"selectPlan('YEARLY', 4000)\">\n" +
            "                <div class=\"plan-name\">Yearly Plan</div>\n" +
            "                <div class=\"plan-price\">KSh 4,000<span style=\"font-size:14px\">/yr</span></div>\n" +
            "                <ul class=\"plan-features\">\n" +
            "                    <li>✓ All monthly features</li>\n" +
            "                    <li>✓ 12 months access</li>\n" +
            "                    <li>✓ Save KSh 2,000</li>\n" +
            "                </ul>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "\n" +
            "        <!-- Payment Methods -->\n" +
            "        <div class=\"payment-tabs\">\n" +
            "            <button class=\"payment-tab active\" onclick=\"showPaymentMethod('mpesa')\">📱 M-Pesa</button>\n" +
            "            <button class=\"payment-tab\" onclick=\"showPaymentMethod('paypal')\">🅿️ PayPal</button>\n" +
            "            <button class=\"payment-tab\" onclick=\"showPaymentMethod('stripe')\">💳 Stripe</button>\n" +
            "            <button class=\"payment-tab\" onclick=\"showPaymentMethod('bank')\">🏦 Bank</button>\n" +
            "        </div>\n" +
            "\n" +
            "        <!-- M-Pesa Form -->\n" +
            "        <div id=\"mpesa\" class=\"payment-form active\">\n" +
            "            <div class=\"mpesa-info\">\n" +
            "                <h3>📱 Pay with M-Pesa</h3>\n" +
            "                <p>Pay securely using your mobile money</p>\n" +
            "            </div>\n" +
            "            <div class=\"form-group\">\n" +
            "                <label>Phone Number</label>\n" +
            "                <input type=\"tel\" id=\"mpesaPhone\" placeholder=\"e.g., 254712345678\" required pattern=\"254[0-9]{9}\">\n" +
            "            </div>\n" +
            "            <button class=\"btn\" onclick=\"payWithMpesa()\">Pay with M-Pesa</button>\n" +
            "            <p class=\"note\">You will receive an STK push. Enter PIN to complete.</p>\n" +
            "        </div>\n" +
            "\n" +
            "        <!-- PayPal Form -->\n" +
            "        <div id=\"paypal\" class=\"payment-form\">\n" +
            "            <div class=\"paypal-info\">\n" +
            "                <h3>🅿️ Pay with PayPal</h3>\n" +
            "                <p>Pay securely using your PayPal account</p>\n" +
            "            </div>\n" +
            "            <div class=\"form-group\">\n" +
            "                <label>PayPal Email</label>\n" +
            "                <input type=\"email\" id=\"paypalEmail\" placeholder=\"your@email.com\">\n" +
            "            </div>\n" +
            "            <button class=\"btn\" onclick=\"alert('PayPal integration coming soon! Contact support for PayPal payments.')\">Continue to PayPal</button>\n" +
            "            <p class=\"note\">You will be redirected to PayPal to complete payment.</p>\n" +
            "        </div>\n" +
            "\n" +
            "        <!-- Stripe Form -->\n" +
            "        <div id=\"stripe\" class=\"payment-form\">\n" +
            "            <div class=\"stripe-info\">\n" +
            "                <h3>💳 Pay with Card (Stripe)</h3>\n" +
            "                <p>Pay securely using Visa, Mastercard, or other cards</p>\n" +
            "            </div>\n" +
            "            <div class=\"form-group\">\n" +
            "                <label>Card Number</label>\n" +
            "                <input type=\"text\" id=\"cardNumber\" placeholder=\"1234 5678 9012 3456\" maxlength=\"19\">\n" +
            "            </div>\n" +
            "            <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 15px;\">\n" +
            "                <div class=\"form-group\">\n" +
            "                    <label>Expiry Date</label>\n" +
            "                    <input type=\"text\" id=\"cardExpiry\" placeholder=\"MM/YY\" maxlength=\"5\">\n" +
            "                </div>\n" +
            "                <div class=\"form-group\">\n" +
            "                    <label>CVC</label>\n" +
            "                    <input type=\"text\" id=\"cardCvc\" placeholder=\"123\" maxlength=\"4\">\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <button class=\"btn\" onclick=\"alert('Stripe integration coming soon! Contact support for card payments.')\">Pay Now</button>\n" +
            "            <p class=\"note\">Your card details are secure with Stripe.</p>\n" +
            "        </div>\n" +
            "\n" +
            "        <!-- Bank Transfer Form -->\n" +
            "        <div id=\"bank\" class=\"payment-form\">\n" +
            "            <div class=\"bank-info\">\n" +
            "                <h3>🏦 Bank Transfer</h3>\n" +
            "                <p>Transfer directly to our bank account</p>\n" +
            "            </div>\n" +
            "            <div class=\"bank-details\">\n" +
            "                <p><strong>Bank Name:</strong> Kenya Commercial Bank (KCB)</p>\n" +
            "                <p><strong>Account Name:</strong> Basic Computer Ltd</p>\n" +
            "                <p><strong>Account Number:</strong> 1234567890</p>\n" +
            "                <p><strong>Branch:</strong> Nairobi Main</p>\n" +
            "                <p><strong>SWIFT Code:</strong> KCBLKEN</p>\n" +
            "            </div>\n" +
            "            <div class=\"form-group\" style=\"margin-top: 20px;\">\n" +
            "                <label>Your Email (for confirmation)</label>\n" +
            "                <input type=\"email\" id=\"bankEmail\" placeholder=\"your@email.com\">\n" +
            "            </div>\n" +
            "            <div class=\"form-group\">\n" +
            "                <label>Transaction Reference</label>\n" +
            "                <input type=\"text\" id=\"transactionRef\" placeholder=\"Enter your bank transaction ID\">\n" +
            "            </div>\n" +
            "            <button class=\"btn\" onclick=\"submitBankTransfer()\">Confirm Payment</button>\n" +
            "            <p class=\"note\">We'll confirm your payment within 24 hours.</p>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div id=\"successMsg\" class=\"success-msg\">\n" +
            "            ✓ Payment submitted successfully! We'll confirm your subscription soon.\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        let selectedPlan = 'MONTHLY';\n" +
            "        let selectedAmount = 500;\n" +
            "        \n" +
            "        function selectPlan(plan, amount) {\n" +
            "            selectedPlan = plan;\n" +
            "            selectedAmount = amount;\n" +
            "            document.querySelectorAll('.plan').forEach(p => p.classList.remove('selected'));\n" +
            "            event.currentTarget.classList.add('selected');\n" +
            "        }\n" +
            "        \n" +
            "        function showPaymentMethod(method) {\n" +
            "            document.querySelectorAll('.payment-form').forEach(f => f.classList.remove('active'));\n" +
            "            document.querySelectorAll('.payment-tab').forEach(t => t.classList.remove('active'));\n" +
            "            document.getElementById(method).classList.add('active');\n" +
            "            event.target.classList.add('active');\n" +
            "        }\n" +
            "        \n" +
            "        function payWithMpesa() {\n" +
            "            const phone = document.getElementById('mpesaPhone').value;\n" +
            "            if (!phone || !phone.match(/^254[0-9]{9}$/)) {\n" +
            "                alert('Please enter a valid M-Pesa phone number (254XXXXXXXXX)');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            fetch('/api/mpesa/stkpush?phone=' + phone + '&amount=' + selectedAmount + '&plan=' + selectedPlan, {\n" +
            "                method: 'POST'\n" +
            "            })\n" +
            "            .then(response => response.json())\n" +
            "            .then(data => {\n" +
            "                if (data.success) {\n" +
            "                    document.getElementById('successMsg').style.display = 'block';\n" +
            "                    alert('STK push sent! Enter your PIN to complete payment.');\n" +
            "                } else {\n" +
            "                    alert('Error: ' + data.message);\n" +
            "                }\n" +
            "            })\n" +
            "            .catch(error => {\n" +
            "                alert('Payment request failed. Please try again.');\n" +
            "            });\n" +
            "        }\n" +
            "        \n" +
            "        function submitBankTransfer() {\n" +
            "            const email = document.getElementById('bankEmail').value;\n" +
            "            const ref = document.getElementById('transactionRef').value;\n" +
            "            \n" +
            "            if (!email || !ref) {\n" +
            "                alert('Please fill in all fields');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            document.getElementById('successMsg').style.display = 'block';\n" +
            "            alert('Bank transfer details submitted! We will verify and activate your subscription within 24 hours.');\n" +
            "        }\n" +
            "        \n" +
            "        // Format card number\n" +
            "        document.getElementById('cardNumber').addEventListener('input', function(e) {\n" +
            "            let value = e.target.value.replace(/\\s/g, '');\n" +
            "            let formatted = value.match(/.{1,4}/g)?.join(' ') || value;\n" +
            "            e.target.value = formatted;\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
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
