package com.chefmate.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiApiUrl;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String geminiModel;

    public AiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        logger.info("=== Gemini AI Service Initialization ===");
        logger.info("API Key configured: {}", geminiApiKey != null && !geminiApiKey.trim().isEmpty());
        logger.info("API Key length: {}", geminiApiKey != null ? geminiApiKey.length() : 0);
        logger.info("API URL: {}", geminiApiUrl);
        logger.info("Model: {}", geminiModel);
        
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            try {
                listAvailableModels();
            } catch (Exception e) {
                logger.warn("Could not list available models: {}", e.getMessage());
            }
        }
        
        logger.info("=========================================");
    }
    
    private void listAvailableModels() {
        try {
            String listUrl = UriComponentsBuilder.fromHttpUrl(geminiApiUrl + "/models")
                    .queryParam("key", geminiApiKey)
                    .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            ResponseEntity<String> response = restTemplate.exchange(
                    listUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                logger.info("Available Gemini models:");
                if (jsonResponse.has("models")) {
                    jsonResponse.get("models").forEach(model -> {
                        String name = model.path("name").asText();
                        logger.info("  - {}", name);
                    });
                }
            }
        } catch (Exception e) {
            logger.warn("Error listing models: {}", e.getMessage());
        }
    }

    public String getAiResponse(String userMessage, Long recipeId) {
        return getAiResponse(userMessage, recipeId, null);
    }
    
    public String getAiResponse(String userMessage, Long recipeId, Map<String, Object> cookingContext) {
        logger.info("Gemini API key status: {}", 
            geminiApiKey != null && !geminiApiKey.trim().isEmpty() 
                ? "Configured (length: " + geminiApiKey.length() + ")" 
                : "NOT CONFIGURED");
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            logger.warn("Gemini API key is not configured. Returning default response.");
            return getDefaultResponse(userMessage);
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            
            StringBuilder systemInstructionBuilder = new StringBuilder();
            systemInstructionBuilder.append("You are a professional cooking assistant. ");
            systemInstructionBuilder.append("You help people with recipes, cooking tips, techniques, and nutritional advice. ");
            systemInstructionBuilder.append("Be helpful, friendly, and specific. ");
            
            if (cookingContext != null) {
                // Add full recipe information
                if (cookingContext.get("recipeTitle") != null) {
                    systemInstructionBuilder.append("\n\n=== RECIPE INFORMATION ===\n");
                    systemInstructionBuilder.append("Recipe: ").append(cookingContext.get("recipeTitle")).append("\n");
                    
                    if (cookingContext.get("recipeDescription") != null) {
                        systemInstructionBuilder.append("Description: ").append(cookingContext.get("recipeDescription")).append("\n");
                    }
                    
                    if (cookingContext.get("recipeDifficulty") != null) {
                        systemInstructionBuilder.append("Difficulty: ").append(cookingContext.get("recipeDifficulty")).append("\n");
                    }
                    
                    if (cookingContext.get("prepTime") != null) {
                        systemInstructionBuilder.append("Prep Time: ").append(cookingContext.get("prepTime")).append(" minutes\n");
                    }
                    
                    if (cookingContext.get("cookTime") != null) {
                        systemInstructionBuilder.append("Cook Time: ").append(cookingContext.get("cookTime")).append(" minutes\n");
                    }
                    
                    if (cookingContext.get("totalTime") != null) {
                        systemInstructionBuilder.append("Total Time: ").append(cookingContext.get("totalTime")).append(" minutes\n");
                    }
                    
                    if (cookingContext.get("servings") != null) {
                        systemInstructionBuilder.append("Servings: ").append(cookingContext.get("servings")).append("\n");
                    }
                    
                    // Ingredients
                    if (cookingContext.get("recipeIngredients") != null) {
                        @SuppressWarnings("unchecked")
                        List<String> ingredients = (List<String>) cookingContext.get("recipeIngredients");
                        if (ingredients != null && !ingredients.isEmpty()) {
                            systemInstructionBuilder.append("\nIngredients:\n");
                            for (int i = 0; i < ingredients.size(); i++) {
                                systemInstructionBuilder.append("  ").append(i + 1).append(". ").append(ingredients.get(i)).append("\n");
                            }
                        }
                    }
                    
                    // Steps
                    if (cookingContext.get("recipeSteps") != null) {
                        @SuppressWarnings("unchecked")
                        List<String> steps = (List<String>) cookingContext.get("recipeSteps");
                        if (steps != null && !steps.isEmpty()) {
                            systemInstructionBuilder.append("\nCooking Steps:\n");
                            for (int i = 0; i < steps.size(); i++) {
                                systemInstructionBuilder.append("  Step ").append(i + 1).append(": ").append(steps.get(i)).append("\n");
                            }
                        }
                    }
                }
                
                // Current cooking session context
                systemInstructionBuilder.append("\n=== CURRENT COOKING SESSION ===\n");
                if (cookingContext.get("currentStep") != null && cookingContext.get("totalSteps") != null) {
                    systemInstructionBuilder.append("Current Step: ").append(cookingContext.get("currentStep"))
                            .append("/").append(cookingContext.get("totalSteps")).append("\n");
                }
                if (cookingContext.get("elapsedTimeSeconds") != null) {
                    long elapsedSeconds = ((Number) cookingContext.get("elapsedTimeSeconds")).longValue();
                    long minutes = elapsedSeconds / 60;
                    long seconds = elapsedSeconds % 60;
                    systemInstructionBuilder.append("Elapsed Time: ").append(String.format("%d:%02d", minutes, seconds)).append("\n");
                }
                if (cookingContext.get("usedIngredients") != null) {
                    @SuppressWarnings("unchecked")
                    List<String> usedIngredients = (List<String>) cookingContext.get("usedIngredients");
                    if (usedIngredients != null && !usedIngredients.isEmpty()) {
                        systemInstructionBuilder.append("Used Ingredients: ").append(String.join(", ", usedIngredients)).append("\n");
                    }
                }
                if (cookingContext.get("cookingStage") != null) {
                    systemInstructionBuilder.append("Cooking Stage: ").append(cookingContext.get("cookingStage")).append("\n");
                }
                if (cookingContext.get("currentAction") != null) {
                    systemInstructionBuilder.append("Current Action: ").append(cookingContext.get("currentAction")).append("\n");
                }
                if (cookingContext.get("stoveSetting") != null) {
                    systemInstructionBuilder.append("Stove Setting: ").append(cookingContext.get("stoveSetting")).append("\n");
                }
                
                systemInstructionBuilder.append("\n=== INSTRUCTIONS ===\n");
                systemInstructionBuilder.append("You have access to the FULL recipe information above. ");
                systemInstructionBuilder.append("Use this information to provide accurate, specific guidance. ");
                systemInstructionBuilder.append("If the user asks about ingredients, steps, timing, or techniques, refer to the recipe details. ");
                systemInstructionBuilder.append("If the user says 'next step', help them move to the next step. ");
                systemInstructionBuilder.append("If they have a problem, provide specific solutions based on the current context and recipe information.");
            }
            
            systemInstructionBuilder.append("\nIMPORTANT: Always respond STRICTLY in English, regardless of what language the question is asked in. ");
            systemInstructionBuilder.append("If the question is in another language, still respond in English.");
            
            String systemInstruction = systemInstructionBuilder.toString();
            
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            parts.add(Map.of("text", userMessage));
            content.put("parts", parts);
            contents.add(content);
            
            requestBody.put("contents", contents);
            
            Map<String, Object> systemInstructionMap = new HashMap<>();
            systemInstructionMap.put("parts", List.of(Map.of("text", systemInstruction)));
            requestBody.put("systemInstruction", systemInstructionMap);
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1000);
            requestBody.put("generationConfig", generationConfig);

            String url = UriComponentsBuilder.fromHttpUrl(geminiApiUrl + "/models/" + geminiModel + ":generateContent")
                    .queryParam("key", geminiApiKey)
                    .toUriString();
            
            logger.info("Using Gemini API URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("Calling Gemini API with URL: {}", url);
            logger.info("Request body: {}", objectMapper.writeValueAsString(requestBody));
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            logger.info("Gemini API response status: {}", response.getStatusCode());
            logger.info("Gemini API response body: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Gemini API returned error status: {}", response.getStatusCode());
                return "Error communicating with AI: " + response.getStatusCode() + ". Please check logs for more information.";
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            if (!jsonResponse.has("candidates") || jsonResponse.get("candidates").size() == 0) {
                logger.error("Gemini API response has no candidates. Full response: {}", response.getBody());
                return "Error: AI did not return a response. Please check logs.";
            }
            
            String aiResponse = jsonResponse
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            logger.info("Received AI response successfully from Gemini: {}", aiResponse.substring(0, Math.min(100, aiResponse.length())));
            return aiResponse;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP error calling Gemini API: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "Error communicating with AI: " + e.getStatusCode() + ". " + 
                   (e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : e.getMessage());
        } catch (Exception e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
            logger.error("Exception type: {}", e.getClass().getName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            return "Error communicating with AI: " + e.getMessage() + ". Please check logs for more information.";
        }
    }

    private String getDefaultResponse(String userMessage) {
        return "Hello! I'm your cooking assistant. " +
                "Currently, the AI functionality is not configured with a Google Gemini API key. " +
                "To activate the full AI functionality, you need to add a Gemini API key in the application.yaml file or as an environment variable GEMINI_API_KEY. " +
                "Your message was: \"" + userMessage + "\". " +
                "I can help you with recipes, cooking tips, techniques, and much more, once the API key is configured! " +
                "Gemini API has a FREE tier with up to 60 requests per minute!";
    }
}
