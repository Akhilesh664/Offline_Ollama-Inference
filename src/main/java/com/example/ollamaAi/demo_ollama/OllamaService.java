package com.example.ollamaAi.demo_ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service class that handles communication with the Ollama API.
 * This service is responsible for:
 * 1. Managing HTTP connections to the Ollama API
 * 2. Handling request/response serialization/deserialization
 * 3. Implementing retry logic for transient failures
 * 4. Managing timeouts and error handling
 */

@Service
public class OllamaService {

    // ObjectMapper for JSON serialization/deserialization
    // Thread-safe and can be shared across requests
    private final ObjectMapper objectMapper = new ObjectMapper();

    // RetryTemplate handles automatic retries for failed operations
    // Configured with exponential backoff in RetryConfig
    private final RetryTemplate retryTemplate;

    // Configuration properties with default values
    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String ollamaApiUrl;

    @Value("${ollama.model:deepseek-coder:1.3b}")
    private String model;

    // Timeout in seconds for API calls (default: 5 minutes)
    @Value("${ollama.timeout.seconds:300}") // 5 minutes default
    private int timeoutSeconds;

    /**
     * Constructor for dependency injection.
     * @param retryTemplate Configured retry template from Spring context
     */
    public OllamaService(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }


    /**
     * Sends a prompt to the Ollama API and returns the generated response.
     *
     * This method:
     * 1. Validates the input prompt
     * 2. Establishes an HTTP connection to the Ollama API
     * 3. Sends the prompt with configuration
     * 4. Handles the response and errors
     * 5. Implements retry logic for transient failures
     *
     * The @Retryable annotation will automatically retry the method if it throws
     * SocketTimeoutException or IOException, with exponential backoff.
     *
     * Retry behavior:
     * - Max 3 attempts (initial + 2 retries)
     * - 1 second initial delay
     * - Exponential backoff with multiplier of 2
     * - Max delay of 10 seconds between retries
     *
     * @param prompt The input text prompt for the AI model
     * @param model The model to use for the AI
     * @return The generated response from the Ollama model
     * @throws IOException If there's an I/O error during the API call
     * @throws IllegalArgumentException If the prompt is empty or null
     */
    @Retryable(
            value = {SocketTimeoutException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String askOllama(String prompt, String model) throws IOException {
        // Input validation
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt cannot be empty or null");
        }
        
        // Use default model if none specified
        String modelToUse = StringUtils.hasText(model) ? model : this.model;

        // 1. Set up the HTTP connection
        URL url = new URL(ollamaApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Configure connection settings
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        // Set timeouts to prevent hanging indefinitely
        connection.setConnectTimeout(timeoutSeconds * 1000); // connection timeout
        connection.setReadTimeout(timeoutSeconds * 1000); // read timeout

        try {
            // Create request body
            // 2. Create and send the request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelToUse); // AI model to use (either from request or default)
            requestBody.put("prompt", prompt); // user's input prompt
            requestBody.put("stream", false); // We want a single response, not a stream

            // Convert the request body to JSON string
            String jsonInput = objectMapper.writeValueAsString(requestBody);

            // 3. Write the JSON payload to the connection
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 4. Check the HTTP response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            // 5. Parse the JSON response
            JsonNode jsonNode = objectMapper.readTree(connection.getInputStream());
            if (!jsonNode.has("response")) {
                throw new IOException("Invalid response format from Ollama API");
            }

            // 6. Return the generated text
            return jsonNode.get("response").asText();
        } finally {
            // 7. Always ensure the connection is closed to prevent resource leaks
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}