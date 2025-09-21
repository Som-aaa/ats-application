package com.ats.utils;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
public class ApiKeyReader {
    
    private static final String API_KEY_FILE = "api-key.txt";
    
    public String readApiKey() {
        try {
            if (!Files.exists(Paths.get(API_KEY_FILE))) {
                System.out.println("WARNING: api-key.txt file not found. Using environment variable or default.");
                return null;
            }
            
            List<String> lines = Files.readAllLines(Paths.get(API_KEY_FILE));
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && 
                    !trimmedLine.startsWith("#") && 
                    !trimmedLine.equals("your-openai-api-key-here")) {
                    
                    System.out.println("DEBUG: Found API key in file: " + trimmedLine.substring(0, Math.min(7, trimmedLine.length())) + "...");
                    return trimmedLine;
                }
            }
            
            System.out.println("WARNING: No valid API key found in api-key.txt");
            return null;
            
        } catch (IOException e) {
            System.out.println("ERROR: Could not read api-key.txt: " + e.getMessage());
            return null;
        }
    }
} 