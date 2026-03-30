package com.basiccomputer.handler;

import com.basiccomputer.db.Database;
import com.basiccomputer.util.TemplateUtil;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class ContentHandler implements HttpHandler {
    private Map<String, String> sessions;
    
    public ContentHandler(Map<String, String> sessions) {
        this.sessions = sessions;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String userEmail = getUserFromSession(exchange);
        
        String query = exchange.getRequestURI().getQuery();
        String type = getQueryParam(query, "type");
        String category = getQueryParam(query, "category");
        String id = getQueryParam(query, "id");
        String search = getQueryParam(query, "search");
        String page = getQueryParam(query, "page");
        
        if (id != null) {
            showContentDetail(exchange, id);
            return;
        }
        
        int currentPage = 1;
        int itemsPerPage = 12;
        try {
            if (page != null) currentPage = Integer.parseInt(page);
        } catch (Exception e) { currentPage = 1; }
        
        List<Map<String, Object>> contentList = new ArrayList<>();
        int totalItems = 0;
        try {
            totalItems = Database.getContentCount(type, search);
            int offset = (currentPage - 1) * itemsPerPage;
            contentList = Database.getAllContentPaginated(type, category, search, itemsPerPage, offset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        
        String contentHtml = buildContentGrid(contentList, currentPage, totalPages, type, category, search);
        String html = buildContentPage(contentHtml, type, category, search, currentPage, totalPages, totalItems);
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private void showContentDetail(HttpExchange exchange, String id) throws IOException {
        Map<String, Object> content = null;
        try {
            content = Database.getContentById(Integer.parseInt(id));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (content == null) {
            exchange.getResponseHeaders().set("Location", "/content");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        String html = TemplateUtil.loadTemplate("detail");
        
        String contentType = (String) content.get("type");
        String icon = getIconForType(contentType);
        String body = (String) content.get("body");
        
        html = html.replace("CONTENT_TITLE", (String) content.get("title"));
        html = html.replace("CONTENT_ICON", icon);
        html = html.replace("CONTENT_TYPE", getTypeLabel(contentType));
        html = html.replace("CONTENT_CATEGORY", formatCategory((String) content.get("category")));
        html = html.replace("CONTENT_VIEWS", String.valueOf(new Random().nextInt(500) + 50));
        html = html.replace("CONTENT_RATING", String.valueOf(new Random().nextInt(3) + 3));
        html = html.replace("CONTENT_DESCRIPTION", (String) content.get("description"));
        html = html.replace("CONTENT_BODY", body);
        html = html.replace("CONCEPT_TAGS", getConceptTags(body));
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    private String getConceptTags(String body) {
        String[] words = body.split("[,.:;\\s]+");
        Set<String> concepts = new LinkedHashSet<>();
        for (String word : words) {
            if (word.length() > 4 && !word.toLowerCase().contains("example")) {
                concepts.add(word.trim());
                if (concepts.size() >= 8) break;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (String concept : concepts) {
            sb.append("<span class=\"concept-tag\">").append(concept).append("</span>");
        }
        return sb.toString();
    }
    
    private String buildContentPage(String contentHtml, String type, String category, String search, int currentPage, int totalPages, int totalItems) {
        String html = TemplateUtil.loadTemplate("content");
        
        String searchValue = search != null ? search : "";
        String typeValue = type != null ? type : "";
        String categoryValue = category != null ? category : "";
        
        // Basic placeholders
        html = html.replace("SEARCH_VALUE", searchValue);
        html = html.replace("CONTENT_TYPE", typeValue);
        html = html.replace("SEARCH_CAT", categoryValue);
        
        // Title based on filters
        String title = "Study Materials";
        String subtitle = "Access all your Computer Science learning resources";
        if (category != null && !category.isEmpty()) {
            title = formatCategory(category);
            subtitle = "Browse " + formatCategory(category) + " study materials";
        }
        if (type != null && !type.isEmpty()) {
            title = getTypeLabel(type) + "s";
            if (category != null && !category.isEmpty()) {
                title = formatCategory(category) + " " + getTypeLabel(type) + "s";
            }
        }
        html = html.replace("STUDY_TITLE", title);
        html = html.replace("STUDY_SUBTITLE", subtitle);
        
        // Category sidebar active states
        html = html.replace("CAT_ALL", category == null || category.isEmpty() ? "active" : "");
        html = html.replace("CAT_MS_WORD", "MS_WORD".equals(category) ? "active" : "");
        html = html.replace("CAT_MS_EXCEL", "MS_EXCEL".equals(category) ? "active" : "");
        html = html.replace("CAT_MS_ACCESS", "MS_ACCESS".equals(category) ? "active" : "");
        html = html.replace("CAT_MS_POWERPOINT", "MS_POWERPOINT".equals(category) ? "active" : "");
        html = html.replace("CAT_MS_PUBLISHER", "MS_PUBLISHER".equals(category) ? "active" : "");
        html = html.replace("CAT_INTERNET_EMAIL", "INTERNET_EMAIL".equals(category) ? "active" : "");
        html = html.replace("CAT_EMERGING_TRENDS", "EMERGING_TRENDS".equals(category) ? "active" : "");
        html = html.replace("CAT_PROGRAMMING", "PROGRAMMING".equals(category) ? "active" : "");
        html = html.replace("CAT_DATA_STRUCTURES", "DATA_STRUCTURES".equals(category) ? "active" : "");
        html = html.replace("CAT_ALGORITHMS", "ALGORITHMS".equals(category) ? "active" : "");
        html = html.replace("CAT_DATABASES", "DATABASES".equals(category) ? "active" : "");
        html = html.replace("CAT_WEB_DEVELOPMENT", "WEB_DEVELOPMENT".equals(category) ? "active" : "");
        html = html.replace("CAT_OPERATING_SYSTEMS", "OPERATING_SYSTEMS".equals(category) ? "active" : "");
        html = html.replace("CAT_COMPUTER_NETWORKS", "COMPUTER_NETWORKS".equals(category) ? "active" : "");
        html = html.replace("CAT_SOFTWARE_ENGINEERING", "SOFTWARE_ENGINEERING".equals(category) ? "active" : "");
        html = html.replace("CAT_ARTIFICIAL_INTELLIGENCE", "ARTIFICIAL_INTELLIGENCE".equals(category) ? "active" : "");
        html = html.replace("CAT_COMPUTER_ARCHITECTURE", "COMPUTER_ARCHITECTURE".equals(category) ? "active" : "");
        html = html.replace("CAT_MATHEMATICS", "MATHEMATICS".equals(category) ? "active" : "");
        
        // Type tab active states
        html = html.replace("TAB_ALL", type == null ? "active" : "");
        html = html.replace("TAB_NOTES", "NOTE".equals(type) ? "active" : "");
        html = html.replace("TAB_VIDEOS", "VIDEO".equals(type) ? "active" : "");
        html = html.replace("TAB_EXAMS", "EXAM".equals(type) ? "active" : "");
        html = html.replace("TAB_QUESTIONS", "SAMPLE_QUESTION".equals(type) ? "active" : "");
        
        // Active filters display
        html = html.replace("ACTIVE_FILTERS_HTML", buildActiveFilters(type, category, search));
        
        // Results count
        html = html.replace("RESULTS_COUNT_HTML", buildResultsCount(totalItems, type, category, currentPage, totalPages));
        
        // Content grid
        if (!contentHtml.isEmpty()) {
            html = html.replace("CONTENT_HTML", contentHtml);
            html = html.replace("NO_CONTENT_HTML", "");
        } else {
            html = html.replace("CONTENT_HTML", "");
            html = html.replace("NO_CONTENT_HTML", 
                "<div class=\"no-content\">\n" +
                "    <div class=\"no-content-icon\">📚</div>\n" +
                "    <h2>No materials found</h2>\n" +
                "    <p>Try adjusting your filters or search terms</p>\n" +
                "    <a href=\"/content\" class=\"view-btn\">View All Content</a>\n" +
                "</div>");
        }
        
        // Pagination
        if (totalPages > 1) {
            String pagination = buildPagination(currentPage, totalPages, type, category, search);
            html = html.replace("PAGINATION_HTML", pagination);
        } else {
            html = html.replace("PAGINATION_HTML", "");
        }
        
        return html;
    }
    
    private String buildActiveFilters(String type, String category, String search) {
        if ((type == null || type.isEmpty()) && (category == null || category.isEmpty()) && (search == null || search.isEmpty())) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"active-filters\">");
        sb.append("<span class=\"filter-label\">Filters:</span>");
        
        if (category != null && !category.isEmpty()) {
            String clearUrl = "/content" + (type != null ? "?type=" + type : "");
            sb.append("<span class=\"filter-tag\">").append(formatCategory(category));
            sb.append(" <a href=\"").append(clearUrl).append("\" class=\"remove-filter\">&times;</a></span>");
        }
        if (type != null && !type.isEmpty()) {
            String clearUrl = "/content" + (category != null && !category.isEmpty() ? "?category=" + category : "");
            sb.append("<span class=\"filter-tag\">").append(getTypeLabel(type));
            sb.append(" <a href=\"").append(clearUrl).append("\" class=\"remove-filter\">&times;</a></span>");
        }
        if (search != null && !search.isEmpty()) {
            String clearUrl = "/content";
            if (type != null && !type.isEmpty()) clearUrl += "?type=" + type;
            else if (category != null && !category.isEmpty()) clearUrl += "?category=" + category;
            sb.append("<span class=\"filter-tag\">\"").append(search).append("\"");
            sb.append(" <a href=\"").append(clearUrl).append("\" class=\"remove-filter\">&times;</a></span>");
        }
        
        sb.append("<a href=\"/content\" class=\"clear-filters\">Clear all</a>");
        sb.append("</div>");
        return sb.toString();
    }
    
    private String buildResultsCount(int totalItems, String type, String category, int currentPage, int totalPages) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"results-count\">");
        sb.append("<span class=\"results-text\">Showing <strong>").append(totalItems).append("</strong> results");
        if (category != null && !category.isEmpty()) {
            sb.append(" in <strong>").append(formatCategory(category)).append("</strong>");
        }
        if (type != null && !type.isEmpty()) {
            sb.append(" for <strong>").append(getTypeLabel(type)).append("s</strong>");
        }
        sb.append("</span>");
        if (totalPages > 1) {
            sb.append("<span class=\"results-category\">Page ").append(currentPage).append(" of ").append(totalPages).append("</span>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPagination(int currentPage, int totalPages, String type, String category, String search) {
        if (totalPages <= 1) return "";
        
        String baseUrl = "/content?";
        if (type != null && !type.isEmpty()) {
            baseUrl += "type=" + type + "&";
        }
        if (category != null && !category.isEmpty()) {
            baseUrl += "category=" + category + "&";
        }
        if (search != null && !search.isEmpty()) {
            baseUrl += "search=" + search + "&";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"pagination\">");
        
        if (currentPage > 1) {
            sb.append("<a href=\"").append(baseUrl).append("page=").append(currentPage - 1).append("\" class=\"page-btn\">← Previous</a>");
        }
        
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);
        
        for (int i = startPage; i <= endPage; i++) {
            sb.append("<a href=\"").append(baseUrl).append("page=").append(i).append("\" class=\"page-btn ").append(i == currentPage ? "active" : "").append("\">").append(i).append("</a>");
        }
        
        if (currentPage < totalPages) {
            sb.append("<a href=\"").append(baseUrl).append("page=").append(currentPage + 1).append("\" class=\"page-btn\">Next →</a>");
        }
        
        sb.append("</div>");
        return sb.toString();
    }
    
    private String buildContentGrid(List<Map<String, Object>> contentList, int currentPage, int totalPages, String type, String category, String search) {
        if (contentList.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='content-grid'>");
        for (Map<String, Object> content : contentList) {
            String contentType = (String) content.get("type");
            String iconClass = getIconClass(contentType);
            String icon = getIconForType(contentType);
            
            sb.append("<div class='material-card' onclick=\"window.location.href='/content?id=" + content.get("id") + "'\">");
            sb.append("<div class='material-icon " + iconClass + "'>" + icon + "</div>");
            sb.append("<span class='material-category'>" + formatCategory((String) content.get("category")) + "</span>");
            sb.append("<h3 class='material-title'>" + content.get("title") + "</h3>");
            sb.append("<p class='material-desc'>" + content.get("description") + "</p>");
            sb.append("<div class='material-meta'>");
            sb.append("<span>👁️ " + (new Random().nextInt(500) + 50) + " views</span>");
            sb.append("<span class='view-btn'>View →</span>");
            sb.append("</div>");
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }
    
    private String formatCategory(String category) {
        if (category == null) return "";
        switch (category) {
            case "OFFICE_APPLICATIONS": return "Office Applications";
            case "MS_WORD": return "MS Word";
            case "MS_EXCEL": return "MS Excel";
            case "MS_ACCESS": return "MS Access";
            case "MS_POWERPOINT": return "MS PowerPoint";
            case "MS_PUBLISHER": return "MS Publisher";
            case "INTERNET_EMAIL": return "Internet & Email";
            case "EMERGING_TRENDS": return "Emerging Trends";
            case "DATA_STRUCTURES": return "Data Structures";
            case "WEB_DEVELOPMENT": return "Web Development";
            case "OPERATING_SYSTEMS": return "Operating Systems";
            case "COMPUTER_NETWORKS": return "Computer Networks";
            case "SOFTWARE_ENGINEERING": return "Software Engineering";
            case "ARTIFICIAL_INTELLIGENCE": return "AI";
            case "COMPUTER_ARCHITECTURE": return "Computer Architecture";
            case "PROGRAMMING": return "Programming";
            case "ALGORITHMS": return "Algorithms";
            case "DATABASES": return "Databases";
            case "MATHEMATICS": return "Mathematics";
            default: return category.replace("_", " ");
        }
    }
    
    private String getIconClass(String type) {
        switch (type) {
            case "NOTE": return "notes";
            case "VIDEO": return "video";
            case "EXAM": return "exam";
            case "SAMPLE_QUESTION": return "question";
            default: return "notes";
        }
    }
    
    private String getIconForType(String type) {
        switch (type) {
            case "NOTE": return "📝";
            case "VIDEO": return "🎥";
            case "EXAM": return "📋";
            case "SAMPLE_QUESTION": return "❓";
            default: return "📚";
        }
    }
    
    private String getTypeLabel(String type) {
        switch (type) {
            case "NOTE": return "Note";
            case "VIDEO": return "Video";
            case "EXAM": return "Exam";
            case "SAMPLE_QUESTION": return "Question";
            default: return "Content";
        }
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
    
    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        String[] params = query.split("&");
        for (String p : params) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return null;
    }
}
