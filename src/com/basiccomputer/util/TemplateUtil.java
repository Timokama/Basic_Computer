package com.basiccomputer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TemplateUtil {
    
    private static final Map<String, String> templateCache = new HashMap<>();
    
    public static String loadTemplate(String templateName) {
        if (templateCache.containsKey(templateName)) {
            return templateCache.get(templateName);
        }
        
        String templatePath = "templates/" + templateName + ".html";
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(templatePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            String template = content.toString();
            templateCache.put(templateName, template);
            return template;
        } catch (IOException e) {
            System.err.println("Error loading template: " + templatePath);
            e.printStackTrace();
            return "<html><body><h1>Template not found: " + templateName + "</h1></body></html>";
        }
    }
    
    public static String render(String templateName, Map<String, String> data) {
        String template = loadTemplate(templateName);
        
        if (data == null || data.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        
        // Remove any remaining placeholders
        result = result.replaceAll("\\{\\{[^}]+\\}\\}", "");
        
        return result;
    }
    
    public static void clearCache() {
        templateCache.clear();
    }
}
