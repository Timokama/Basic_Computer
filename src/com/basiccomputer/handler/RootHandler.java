package com.basiccomputer.handler;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;

public class RootHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = getModernHomepage();
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private String getModernHomepage() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Basic Computer - Master Computer Science</title>\n" +
            "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=Space+Grotesk:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
            "    <style>\n" +
            "        * {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            box-sizing: border-box;\n" +
            "        }\n" +
            "        html {\n" +
            "            scroll-behavior: smooth;\n" +
            "        }\n" +
            "        body {\n" +
            "            font-family: 'Outfit', sans-serif;\n" +
            "            background: #0a0a0f;\n" +
            "            color: #ffffff;\n" +
            "            overflow-x: hidden;\n" +
            "            line-height: 1.6;\n" +
            "        }\n" +
            "        \n" +
            "        /* Animated Background */\n" +
            "        .hero-bg {\n" +
            "            position: fixed;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            width: 100%;\n" +
            "            height: 100vh;\n" +
            "            z-index: -1;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-bg::before {\n" +
            "            content: '';\n" +
            "            position: absolute;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "            background: \n" +
            "                radial-gradient(ellipse 80% 50% at 20% 40%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),\n" +
            "                radial-gradient(ellipse 60% 40% at 80% 20%, rgba(194, 124, 14, 0.2) 0%, transparent 50%),\n" +
            "                radial-gradient(ellipse 50% 50% at 50% 80%, rgba(11, 165, 236, 0.15) 0%, transparent 50%);\n" +
            "        }\n" +
            "        \n" +
            "        .grid-pattern {\n" +
            "            position: absolute;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "            background-image: \n" +
            "                linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),\n" +
            "                linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);\n" +
            "            background-size: 50px 50px;\n" +
            "            animation: gridMove 20s linear infinite;\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes gridMove {\n" +
            "            0% { transform: translate(0, 0); }\n" +
            "            100% { transform: translate(50px, 50px); }\n" +
            "        }\n" +
            "        \n" +
            "        /* Floating Elements */\n" +
            "        .floating {\n" +
            "            position: absolute;\n" +
            "            border-radius: 20px;\n" +
            "            animation: float 6s ease-in-out infinite;\n" +
            "        }\n" +
            "        \n" +
            "        .floating-1 {\n" +
            "            width: 80px;\n" +
            "            height: 80px;\n" +
            "            background: linear-gradient(135deg, rgba(120, 119, 198, 0.3), rgba(197, 94, 224, 0.2));\n" +
            "            border: 1px solid rgba(255,255,255,0.1);\n" +
            "            top: 15%;\n" +
            "            left: 10%;\n" +
            "            animation-delay: 0s;\n" +
            "        }\n" +
            "        \n" +
            "        .floating-2 {\n" +
            "            width: 60px;\n" +
            "            height: 60px;\n" +
            "            background: linear-gradient(135deg, rgba(11, 165, 236, 0.3), rgba(59, 130, 246, 0.2));\n" +
            "            border: 1px solid rgba(255,255,255,0.1);\n" +
            "            top: 60%;\n" +
            "            right: 15%;\n" +
            "            animation-delay: 2s;\n" +
            "        }\n" +
            "        \n" +
            "        .floating-3 {\n" +
            "            width: 100px;\n" +
            "            height: 100px;\n" +
            "            background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(249, 115, 22, 0.2));\n" +
            "            border: 1px solid rgba(255,255,255,0.1);\n" +
            "            bottom: 20%;\n" +
            "            left: 20%;\n" +
            "            animation-delay: 4s;\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes float {\n" +
            "            0%, 100% { transform: translateY(0) rotate(0deg); }\n" +
            "            50% { transform: translateY(-20px) rotate(5deg); }\n" +
            "        }\n" +
            "        \n" +
            "        /* Navigation */\n" +
            "        nav {\n" +
            "            position: fixed;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            right: 0;\n" +
            "            z-index: 100;\n" +
            "            padding: 20px 50px;\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            backdrop-filter: blur(20px);\n" +
            "            background: rgba(10, 10, 15, 0.7);\n" +
            "            border-bottom: 1px solid rgba(255,255,255,0.05);\n" +
            "            transition: all 0.3s;\n" +
            "        }\n" +
            "        \n" +
            "        .logo {\n" +
            "            font-family: 'Space Grotesk', sans-serif;\n" +
            "            font-size: 1.8rem;\n" +
            "            font-weight: 700;\n" +
            "            background: linear-gradient(135deg, #7877c6, #c75ee0);\n" +
            "            -webkit-background-clip: text;\n" +
            "            -webkit-text-fill-color: transparent;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 10px;\n" +
            "        }\n" +
            "        \n" +
            "        .logo-icon {\n" +
            "            width: 45px;\n" +
            "            height: 45px;\n" +
            "            background: linear-gradient(135deg, #7877c6, #c75ee0);\n" +
            "            border-radius: 12px;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            font-size: 1.5rem;\n" +
            "            -webkit-text-fill-color: white;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-links {\n" +
            "            display: flex;\n" +
            "            gap: 30px;\n" +
            "            align-items: center;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-links a {\n" +
            "            color: rgba(255,255,255,0.7);\n" +
            "            text-decoration: none;\n" +
            "            font-weight: 500;\n" +
            "            font-size: 0.95rem;\n" +
            "            transition: all 0.3s;\n" +
            "            position: relative;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-links a::after {\n" +
            "            content: '';\n" +
            "            position: absolute;\n" +
            "            bottom: -5px;\n" +
            "            left: 0;\n" +
            "            width: 0;\n" +
            "            height: 2px;\n" +
            "            background: linear-gradient(90deg, #7877c6, #c75ee0);\n" +
            "            transition: width 0.3s;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-links a:hover {\n" +
            "            color: #fff;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-links a:hover::after {\n" +
            "            width: 100%;\n" +
            "        }\n" +
            "        \n" +
            "        .nav-btn {\n" +
            "            padding: 10px 25px;\n" +
            "            border-radius: 30px;\n" +
            "            font-weight: 600;\n" +
            "            text-decoration: none;\n" +
            "            transition: all 0.3s;\n" +
            "            font-size: 0.9rem;\n" +
            "        }\n" +
            "        \n" +
            "        .btn-outline {\n" +
            "            border: 1px solid rgba(255,255,255,0.2);\n" +
            "            color: #fff;\n" +
            "        }\n" +
            "        \n" +
            "        .btn-outline:hover {\n" +
            "            background: rgba(255,255,255,0.1);\n" +
            "            border-color: rgba(255,255,255,0.4);\n" +
            "        }\n" +
            "        \n" +
            "        .btn-gradient {\n" +
            "            background: linear-gradient(135deg, #7877c6, #c75ee0);\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            box-shadow: 0 10px 30px rgba(120, 119, 198, 0.3);\n" +
            "        }\n" +
            "        \n" +
            "        .btn-gradient:hover {\n" +
            "            transform: translateY(-3px);\n" +
            "            box-shadow: 0 15px 40px rgba(120, 119, 198, 0.4);\n" +
            "        }\n" +
            "        \n" +
            "        /* Hero Section */\n" +
            "        .hero {\n" +
            "            min-height: 100vh;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            padding: 120px 50px 80px;\n" +
            "            position: relative;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-content {\n" +
            "            max-width: 900px;\n" +
            "            text-align: center;\n" +
            "            position: relative;\n" +
            "            z-index: 2;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-badge {\n" +
            "            display: inline-flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "            padding: 8px 20px;\n" +
            "            background: rgba(120, 119, 198, 0.1);\n" +
            "            border: 1px solid rgba(120, 119, 198, 0.3);\n" +
            "            border-radius: 30px;\n" +
            "            font-size: 0.85rem;\n" +
            "            color: #a5a4d9;\n" +
            "            margin-bottom: 30px;\n" +
            "            animation: fadeInDown 0.8s ease-out;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-badge span {\n" +
            "            width: 8px;\n" +
            "            height: 8px;\n" +
            "            background: #7877c6;\n" +
            "            border-radius: 50%;\n" +
            "            animation: pulse 2s infinite;\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes pulse {\n" +
            "            0%, 100% { opacity: 1; }\n" +
            "            50% { opacity: 0.5; }\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes fadeInDown {\n" +
            "            from { opacity: 0; transform: translateY(-30px); }\n" +
            "            to { opacity: 1; transform: translateY(0); }\n" +
            "        }\n" +
            "        \n" +
            "        .hero h1 {\n" +
            "            font-family: 'Space Grotesk', sans-serif;\n" +
            "            font-size: 5rem;\n" +
            "            font-weight: 700;\n" +
            "            line-height: 1.1;\n" +
            "            margin-bottom: 25px;\n" +
            "            animation: fadeInUp 1s ease-out 0.2s backwards;\n" +
            "        }\n" +
            "        \n" +
            "        .hero h1 .gradient {\n" +
            "            background: linear-gradient(135deg, #7877c6 0%, #c75ee0 50%, #0ba5ec 100%);\n" +
            "            -webkit-background-clip: text;\n" +
            "            -webkit-text-fill-color: transparent;\n" +
            "            background-size: 200% 200%;\n" +
            "            animation: gradientMove 5s ease infinite;\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes gradientMove {\n" +
            "            0% { background-position: 0% 50%; }\n" +
            "            50% { background-position: 100% 50%; }\n" +
            "            100% { background-position: 0% 50%; }\n" +
            "        }\n" +
            "        \n" +
            "        @keyframes fadeInUp {\n" +
            "            from { opacity: 0; transform: translateY(30px); }\n" +
            "            to { opacity: 1; transform: translateY(0); }\n" +
            "        }\n" +
            "        \n" +
            "        .hero p {\n" +
            "            font-size: 1.3rem;\n" +
            "            color: rgba(255,255,255,0.6);\n" +
            "            margin-bottom: 40px;\n" +
            "            max-width: 600px;\n" +
            "            margin-left: auto;\n" +
            "            margin-right: auto;\n" +
            "            animation: fadeInUp 1s ease-out 0.4s backwards;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-buttons {\n" +
            "            display: flex;\n" +
            "            gap: 20px;\n" +
            "            justify-content: center;\n" +
            "            animation: fadeInUp 1s ease-out 0.6s backwards;\n" +
            "        }\n" +
            "        \n" +
            "        .hero-buttons a {\n" +
            "            padding: 16px 40px;\n" +
            "            border-radius: 30px;\n" +
            "            font-weight: 600;\n" +
            "            text-decoration: none;\n" +
            "            font-size: 1rem;\n" +
            "            transition: all 0.3s;\n" +
            "        }\n" +
            "        \n" +
            "        /* Features Section */\n" +
            "        .features {\n" +
            "            padding: 80px 50px;\n" +
            "            position: relative;\n" +
            "        }\n" +
            "        \n" +
            "        .features-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(4, 1fr);\n" +
            "            gap: 25px;\n" +
            "            max-width: 1200px;\n" +
            "            margin: 0 auto;\n" +
            "        }\n" +
            "        \n" +
            "        @media (max-width: 900px) {\n" +
            "            .features-grid { grid-template-columns: repeat(2, 1fr); }\n" +
            "            .hero h1 { font-size: 3rem; }\n" +
            "            nav { padding: 15px 20px; }\n" +
            "            .hero { padding: 100px 20px 60px; }\n" +
            "            .hero-buttons { flex-direction: column; align-items: center; }\n" +
            "            .features { padding: 60px 20px; }\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card {\n" +
            "            background: rgba(255, 255, 255, 0.03);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.08);\n" +
            "            border-radius: 24px;\n" +
            "            padding: 35px 25px;\n" +
            "            text-align: center;\n" +
            "            transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);\n" +
            "            position: relative;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card::before {\n" +
            "            content: '';\n" +
            "            position: absolute;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            right: 0;\n" +
            "            height: 3px;\n" +
            "            background: linear-gradient(90deg, #7877c6, #c75ee0);\n" +
            "            transform: scaleX(0);\n" +
            "            transition: transform 0.3s;\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card:hover {\n" +
            "            transform: translateY(-10px);\n" +
            "            background: rgba(255, 255, 255, 0.06);\n" +
            "            border-color: rgba(120, 119, 198, 0.3);\n" +
            "            box-shadow: 0 20px 50px rgba(120, 119, 198, 0.15);\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card:hover::before {\n" +
            "            transform: scaleX(1);\n" +
            "        }\n" +
            "        \n" +
            "        .feature-icon {\n" +
            "            font-size: 3rem;\n" +
            "            margin-bottom: 20px;\n" +
            "            display: inline-block;\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card h3 {\n" +
            "            font-size: 1.1rem;\n" +
            "            font-weight: 600;\n" +
            "            margin-bottom: 10px;\n" +
            "        }\n" +
            "        \n" +
            "        .feature-card p {\n" +
            "            font-size: 0.9rem;\n" +
            "            color: rgba(255,255,255,0.5);\n" +
            "        }\n" +
            "        \n" +
            "        /* Stats Section */\n" +
            "        .stats {\n" +
            "            padding: 60px 50px;\n" +
            "            background: rgba(255,255,255,0.02);\n" +
            "            border-top: 1px solid rgba(255,255,255,0.05);\n" +
            "            border-bottom: 1px solid rgba(255,255,255,0.05);\n" +
            "        }\n" +
            "        \n" +
            "        .stats-grid {\n" +
            "            display: flex;\n" +
            "            justify-content: center;\n" +
            "            gap: 80px;\n" +
            "            max-width: 900px;\n" +
            "            margin: 0 auto;\n" +
            "        }\n" +
            "        \n" +
            "        @media (max-width: 600px) {\n" +
            "            .stats-grid { gap: 40px; flex-wrap: wrap; }\n" +
            "        }\n" +
            "        \n" +
            "        .stat-item {\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        \n" +
            "        .stat-number {\n" +
            "            font-family: 'Space Grotesk', sans-serif;\n" +
            "            font-size: 3rem;\n" +
            "            font-weight: 700;\n" +
            "            background: linear-gradient(135deg, #7877c6, #c75ee0);\n" +
            "            -webkit-background-clip: text;\n" +
            "            -webkit-text-fill-color: transparent;\n" +
            "            display: block;\n" +
            "        }\n" +
            "        \n" +
            "        .stat-label {\n" +
            "            color: rgba(255,255,255,0.5);\n" +
            "            font-size: 0.95rem;\n" +
            "        }\n" +
            "        \n" +
            "        /* Footer */\n" +
            "        footer {\n" +
            "            padding: 40px 50px;\n" +
            "            text-align: center;\n" +
            "            color: rgba(255,255,255,0.4);\n" +
            "            font-size: 0.9rem;\n" +
            "            border-top: 1px solid rgba(255,255,255,0.05);\n" +
            "        }\n" +
            "        \n" +
            "        footer a {\n" +
            "            color: rgba(255,255,255,0.6);\n" +
            "            text-decoration: none;\n" +
            "        }\n" +
            "        \n" +
            "        footer a:hover {\n" +
            "            color: #c75ee0;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <!-- Background -->\n" +
            "    <div class=\"hero-bg\">\n" +
            "        <div class=\"grid-pattern\"></div>\n" +
            "        <div class=\"floating floating-1\"></div>\n" +
            "        <div class=\"floating floating-2\"></div>\n" +
            "        <div class=\"floating floating-3\"></div>\n" +
            "    </div>\n" +
            "\n" +
            "    <!-- Navigation -->\n" +
            "    <nav>\n" +
            "        <div class=\"logo\">\n" +
            "            <div class=\"logo-icon\">💻</div>\n" +
            "            Basic Computer\n" +
            "        </div>\n" +
            "        <div class=\"nav-links\">\n" +
            "            <a href=\"#features\">Features</a>\n" +
            "            <a href=\"#stats\">Statistics</a>\n" +
            "            <a href=\"/login\" class=\"nav-btn btn-outline\">Login</a>\n" +
            "            <a href=\"/register\" class=\"nav-btn btn-gradient\">Get Started</a>\n" +
            "        </div>\n" +
            "    </nav>\n" +
            "\n" +
            "    <!-- Hero Section -->\n" +
            "    <section class=\"hero\">\n" +
            "        <div class=\"hero-content\">\n" +
            "            <div class=\"hero-badge\">\n" +
            "                <span></span>\n" +
            "                Learn Computer Science Online\n" +
            "            </div>\n" +
            "            <h1>\n" +
            "                Master <span class=\"gradient\">Computer Science</span><br>\n" +
            "                Through Interactive Learning\n" +
            "            </h1>\n" +
            "            <p>\n" +
            "                Access comprehensive notes, video tutorials, practice questions, and exams.\n" +
            "                Start your journey to becoming a skilled programmer today.\n" +
            "            </p>\n" +
            "            <div class=\"hero-buttons\">\n" +
            "                <a href=\"/register\" class=\"btn-gradient\" style=\"padding: 18px 45px; display: inline-block; text-decoration: none;\">Start Learning Free</a>\n" +
            "                <a href=\"#features\" class=\"btn-outline\" style=\"padding: 18px 45px; display: inline-block; text-decoration: none;\">Explore Features</a>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </section>\n" +
            "\n" +
            "    <!-- Features Section -->\n" +
            "    <section class=\"features\" id=\"features\">\n" +
            "        <div class=\"features-grid\">\n" +
            "            <div class=\"feature-card\">\n" +
            "                <div class=\"feature-icon\">📚</div>\n" +
            "                <h3>Comprehensive Notes</h3>\n" +
            "                <p>50+ detailed study notes covering programming, data structures, algorithms & more</p>\n" +
            "            </div>\n" +
            "            <div class=\"feature-card\">\n" +
            "                <div class=\"feature-icon\">🎥</div>\n" +
            "                <h3>Video Tutorials</h3>\n" +
            "                <p>Watch 10+ video lessons from expert instructors on key CS topics</p>\n" +
            "            </div>\n" +
            "            <div class=\"feature-card\">\n" +
            "                <div class=\"feature-icon\">❓</div>\n" +
            "                <h3>Practice Questions</h3>\n" +
            "                <p>50+ practice questions with detailed answers to test your knowledge</p>\n" +
            "            </div>\n" +
            "            <div class=\"feature-card\">\n" +
            "                <div class=\"feature-icon\">📝</div>\n" +
            "                <h3>Exams & Answers</h3>\n" +
            "                <p>Take 10+ comprehensive exams with full answer explanations</p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </section>\n" +
            "\n" +
            "    <!-- Stats Section -->\n" +
            "    <section class=\"stats\" id=\"stats\">\n" +
            "        <div class=\"stats-grid\">\n" +
            "            <div class=\"stat-item\">\n" +
            "                <span class=\"stat-number\">50+</span>\n" +
            "                <span class=\"stat-label\">Study Notes</span>\n" +
            "            </div>\n" +
            "            <div class=\"stat-item\">\n" +
            "                <span class=\"stat-number\">10+</span>\n" +
            "                <span class=\"stat-label\">Video Tutorials</span>\n" +
            "            </div>\n" +
            "            <div class=\"stat-item\">\n" +
            "                <span class=\"stat-number\">50+</span>\n" +
            "                <span class=\"stat-label\">Practice Questions</span>\n" +
            "            </div>\n" +
            "            <div class=\"stat-item\">\n" +
            "                <span class=\"stat-number\">10+</span>\n" +
            "                <span class=\"stat-label\">Exams</span>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </section>\n" +
            "\n" +
            "    <!-- Footer -->\n" +
            "    <footer>\n" +
            "        <p>&copy; 2025 Basic Computer. Built with 💜 for CS Students.</p>\n" +
            "    </footer>\n" +
            "\n" +
            "    <script>\n" +
            "        // Smooth scroll for anchor links\n" +
            "        document.querySelectorAll('a[href^=\"#\"]').forEach(anchor => {\n" +
            "            anchor.addEventListener('click', function (e) {\n" +
            "                e.preventDefault();\n" +
            "                document.querySelector(this.getAttribute('href')).scrollIntoView({\n" +
            "                    behavior: 'smooth'\n" +
            "                });\n" +
            "            });\n" +
            "        });\n" +
            "\n" +
            "        // Navbar background on scroll\n" +
            "        window.addEventListener('scroll', function() {\n" +
            "            const nav = document.querySelector('nav');\n" +
            "            if (window.scrollY > 50) {\n" +
            "                nav.style.background = 'rgba(10, 10, 15, 0.9)';\n" +
            "                nav.style.boxShadow = '0 5px 30px rgba(0,0,0,0.3)';\n" +
            "            } else {\n" +
            "                nav.style.background = 'rgba(10, 10, 15, 0.7)';\n" +
            "                nav.style.boxShadow = 'none';\n" +
            "            }\n" +
            "        });\n" +
            "\n" +
            "        // Parallax effect on floating elements\n" +
            "        document.addEventListener('mousemove', function(e) {\n" +
            "            const floatings = document.querySelectorAll('.floating');\n" +
            "            const x = (window.innerWidth - e.pageX * 2) / 100;\n" +
            "            const y = (window.innerHeight - e.pageY * 2) / 100;\n" +
            "            \n" +
            "            floatings.forEach((el, index) => {\n" +
            "                const speed = (index + 1) * 2;\n" +
            "                el.style.transform = `translateX(${x * speed}px) translateY(${y * speed}px)`;\n" +
            "            });\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }
}
