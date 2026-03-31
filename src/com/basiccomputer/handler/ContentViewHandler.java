package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class ContentViewHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public ContentViewHandler(Map<String, String> sessions) {
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
        
        // Parse content ID from path
        String path = exchange.getRequestURI().getPath();
        String idStr = path.replace("/view/", "");
        int contentId = 0;
        try {
            contentId = Integer.parseInt(idStr);
        } catch (Exception e) {
            exchange.getResponseHeaders().set("Location", "/content");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        Map<String, Object> content = null;
        try {
            content = Database.getContentById(contentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (content == null) {
            exchange.getResponseHeaders().set("Location", "/content");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        String type = (String) content.get("type");
        String title = (String) content.get("title");
        String body = (String) content.get("body");
        String category = (String) content.get("category");
        
        // Determine icon and type label
        String icon = "📝";
        String typeLabel = "Note";
        if ("VIDEO".equals(type)) { icon = "🎥"; typeLabel = "Video"; }
        else if ("SAMPLE_QUESTION".equals(type)) { icon = "❓"; typeLabel = "Question"; }
        else if ("EXAM".equals(type)) { icon = "📋"; typeLabel = "Exam"; }
        else if ("EXAMPLE".equals(type)) { icon = "💡"; typeLabel = "Example"; }
        else if ("PRACTICAL".equals(type)) { icon = "🔬"; typeLabel = "Practical"; }
        
        String html = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>" + title + " - Basic Computer</title>" +
            "    <style>" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
            "        body { font-family: 'Segoe UI', sans-serif; background: #f5f7fa; }" +
            "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px 40px; display: flex; justify-content: space-between; align-items: center; }" +
            "        .nav a { color: white; text-decoration: none; margin-left: 20px; padding: 10px 20px; border-radius: 5px; }" +
            "        .container { padding: 40px; max-width: 900px; margin: 0 auto; }" +
            "        .back-link { display: inline-block; margin-bottom: 20px; color: #667eea; text-decoration: none; }" +
            "        .back-link:hover { text-decoration: underline; }" +
            "        .content-box { background: white; padding: 40px; border-radius: 15px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            "        .type-badge { display: inline-block; background: #667eea; color: white; padding: 5px 15px; border-radius: 20px; font-size: 12px; margin-bottom: 15px; }" +
            "        .category-badge { display: inline-block; background: #764ba2; color: white; padding: 5px 15px; border-radius: 20px; font-size: 12px; margin-left: 10px; }" +
            "        h1 { color: #333; font-size: 28px; margin-bottom: 20px; }" +
            "        .content-body { color: #555; line-height: 1.8; font-size: 16px; }" +
            "        .content-body p { margin-bottom: 8px; }" +
            "        .content-body br { display: block; margin: 4px 0; }" +
            "        .content-step { background: #f0f4ff; border-left: 3px solid #667eea; padding: 10px 15px; margin: 10px 0; border-radius: 0 8px 8px 0; font-weight: 500; color: #333; }" +
            "        .content-heading { color: #667eea; font-size: 18px; font-weight: 600; margin: 20px 0 10px; }" +
            "        .table-container { overflow-x: auto; margin: 15px 0; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }" +
            "        .content-table { width: 100%; border-collapse: collapse; background: white; font-size: 14px; }" +
            "        .content-table th { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 12px 16px; text-align: left; font-weight: 600; white-space: nowrap; }" +
            "        .content-table td { padding: 10px 16px; border-bottom: 1px solid #eee; color: #444; }" +
            "        .content-table tr:hover td { background: #f8f9ff; }" +
            "        .content-table tr:last-child td { border-bottom: none; }" +
            "        .content-table tr:nth-child(even) td { background: #fafbff; }" +
            "        .content-table tr:nth-child(even):hover td { background: #f0f2ff; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"header\">" +
            "        <h1>Basic Computer</h1>" +
            "        <nav class=\"nav\">" +
            "            <a href=\"/dashboard\">Dashboard</a>" +
            "            <a href=\"/content\">All Content</a>" +
            "            <a href=\"/payment\">Subscribe</a>" +
            "            <a href=\"/logout\">Logout</a>" +
            "        </nav>" +
            "    </div>" +
            "    <div class=\"container\">" +
            "        <a href=\"/content\" class=\"back-link\">Back to Content</a>" +
            "        <div class=\"content-box\">" +
            "            <span class=\"type-badge\">" + typeLabel + "</span>" +
            "            <span class=\"category-badge\">" + category + "</span>" +
            "            <h1>" + title + "</h1>" +
            "            <div class=\"content-body\">" + convertContentToHtml(body) + "</div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(html.getBytes());
        os.close();
    }
    
    private String getUserFromSession(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("session=")) {
                    return sessions.get(cookie.trim().substring(7));
                }
            }
        }
        return null;
    }
    
    private String convertContentToHtml(String body) {
        if (body == null || body.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        String[] lines = body.split("\\\\n|\\n");
        boolean inTable = false;
        boolean firstDataRow = false;
        java.util.List<String[]> tableRows = new java.util.ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Detect table separator line: +----+----+
            if (line.matches("^\\+[=+-]+\\+$")) {
                if (!inTable) {
                    inTable = true;
                    firstDataRow = true;
                    tableRows.clear();
                }
                continue;
            }
            
            // Detect table data row: | col1 | col2 |
            if (inTable && line.startsWith("|") && line.endsWith("|")) {
                String[] cells = line.substring(1, line.length() - 1).split("\\|");
                for (int c = 0; c < cells.length; c++) {
                    cells[c] = cells[c].trim();
                }
                tableRows.add(cells);
                continue;
            }
            
            // End of table - render it
            if (inTable && (!line.startsWith("|") || !line.endsWith("|")) && !line.matches("^\\+[=+-]+\\+$")) {
                inTable = false;
                result.append(buildHtmlTable(tableRows, firstDataRow));
                tableRows.clear();
                firstDataRow = false;
            }
            
            // Regular text line
            if (!inTable) {
                if (line.isEmpty()) {
                    result.append("<br>");
                } else if (line.startsWith("Objective:") || line.startsWith("Steps:") || 
                           line.startsWith("Deliverable:") || line.startsWith("Formulas:") ||
                           line.startsWith("Conditional Formatting:") || line.startsWith("Result:") ||
                           line.startsWith("Input ")) {
                    result.append("<div class=\"content-step\">").append(escapeHtml(line)).append("</div>");
                } else if (line.endsWith(":") && line.length() < 60) {
                    result.append("<div class=\"content-heading\">").append(escapeHtml(line)).append("</div>");
                } else {
                    result.append("<p>").append(escapeHtml(line)).append("</p>");
                }
            }
        }
        
        // Handle table at end of content
        if (inTable && !tableRows.isEmpty()) {
            result.append(buildHtmlTable(tableRows, firstDataRow));
        }
        
        return result.toString();
    }
    
    private String buildHtmlTable(java.util.List<String[]> rows, boolean firstRowIsHeader) {
        if (rows.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"table-container\"><table class=\"content-table\">");
        
        for (int i = 0; i < rows.size(); i++) {
            String[] cells = rows.get(i);
            boolean isHeader = firstRowIsHeader && i == 0;
            
            boolean isDashRow = true;
            for (String cell : cells) {
                if (!cell.matches("^[-]*$") && !cell.isEmpty()) {
                    isDashRow = false;
                    break;
                }
            }
            if (isDashRow) continue;
            
            sb.append("<tr>");
            for (String cell : cells) {
                if (isHeader) {
                    sb.append("<th>").append(escapeHtml(cell)).append("</th>");
                } else {
                    sb.append("<td>").append(escapeHtml(cell)).append("</td>");
                }
            }
            sb.append("</tr>");
        }
        
        sb.append("</table></div>");
        return sb.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}