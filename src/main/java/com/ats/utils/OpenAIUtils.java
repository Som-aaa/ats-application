package com.ats.utils;

import com.ats.exception.OpenAIException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAIUtils {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-tokens:1500}")
    private int maxTokens;

    @Value("${openai.temperature:0.1}")
    private double temperature;

    @Value("${openai.timeout:30000}")
    private int timeoutMs;

    @Value("${openai.retry-attempts:3}")
    private int retryAttempts;

    public String callOpenAI(String apiKey, String prompt) throws OpenAIException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new OpenAIException("OpenAI API key is not configured");
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            throw new OpenAIException("Prompt cannot be empty");
        }

        logger.debug("Calling OpenAI API with model: {}, maxTokens: {}, temperature: {}", 
            model, maxTokens, temperature);

        WebClient client = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        return executeWithRetry(client, requestBody);
    }

    private String executeWithRetry(WebClient client, Map<String, Object> requestBody) throws OpenAIException {
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.debug("OpenAI API attempt {}/{}", attempt, retryAttempts);
                
                String response = client.post()
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();

                return parseResponse(response);

            } catch (WebClientResponseException e) {
                lastException = e;
                logger.warn("OpenAI API error on attempt {}/{}: {} - {}", 
                    attempt, retryAttempts, e.getStatusCode(), e.getResponseBodyAsString());
                
                if (e.getStatusCode().is4xxClientError()) {
                    // Don't retry on client errors
                    throw new OpenAIException("OpenAI API client error: " + e.getResponseBodyAsString(), e);
                }
                
                if (attempt == retryAttempts) {
                    throw new OpenAIException("OpenAI API server error after " + retryAttempts + " attempts: " + e.getResponseBodyAsString(), e);
                }
                
                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new OpenAIException("Request interrupted", ie);
                }
                
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error on attempt {}/{}", attempt, retryAttempts, e);
                
                if (attempt == retryAttempts) {
                    throw new OpenAIException("OpenAI API call failed after " + retryAttempts + " attempts", e);
                }
            }
        }

        throw new OpenAIException("OpenAI API call failed after " + retryAttempts + " attempts", lastException);
    }

    private String parseResponse(String response) throws OpenAIException {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(response);
            
            // Check for errors in response
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = errorNode.has("message") ? 
                    errorNode.get("message").asText() : "Unknown OpenAI error";
                throw new OpenAIException("OpenAI API returned error: " + errorMessage);
            }
            
            // Extract content from choices
            JsonNode choices = jsonNode.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new OpenAIException("No choices returned from OpenAI API");
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message == null) {
                throw new OpenAIException("No message in OpenAI API response");
            }
            
            String content = message.get("content").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new OpenAIException("Empty content returned from OpenAI API");
            }
            
            logger.debug("OpenAI API response received successfully");
            return cleanFormat(content);
            
        } catch (JsonProcessingException e) {
            logger.error("Error parsing OpenAI API response JSON", e);
            throw new OpenAIException("Failed to parse OpenAI API response JSON", e);
        } catch (Exception e) {
            if (e instanceof OpenAIException) {
                throw e;
            }
            logger.error("Error parsing OpenAI API response", e);
            throw new OpenAIException("Failed to parse OpenAI API response", e);
        }
    }

    private static String cleanFormat(String content) {
        if (content == null) {
            return "";
        }
        
        return content.trim()
                .replaceAll("(?i)\\s*career summary\\s*[:：]", "1. Career Summary:")
                .replaceAll("(?i)\\s*ats score\\s*[:：]", "2. ATS Score:")
                .replaceAll("(?i)\\s*strengths and weaknesses\\s*[:：]", "3. Strengths and Weaknesses:")
                .replaceAll("(?i)\\s*suggestions to improve\\s*[:：]", "4. Suggestions to Improve:")
                .replaceAll("\\n{2,}", "\n\n"); // Remove extra newlines
    }
}
