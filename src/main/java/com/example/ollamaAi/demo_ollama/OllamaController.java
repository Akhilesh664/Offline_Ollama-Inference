package com.example.ollamaAi.demo_ollama;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller that handles HTTP requests for the Ollama AI service.
 *
 * This controller:
 * 1. Exposes a GET endpoint for sending prompts to the Ollama AI
 * 2. Handles request validation and error responses
 * 3. Manages HTTP status codes and response formats
 * 4. Provides global exception handling for the API
 *
 * Base URL: /api/ai
 */
@RestController
@RequestMapping("/api/ai")
public class OllamaController {

    // Service layer dependency for AI operations
    private final OllamaService ollamaService;

    /**
     * Constructor for dependency injection.
     * @param ollamaService The service responsible for AI operations
     */
    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Handles GET requests to /api/ai/ask endpoint.
     *
     * This endpoint:
     * 1. Accepts a required 'prompt' query parameter
     * 2. Forwards the prompt to the Ollama AI service
     * 3. Returns the AI-generated response
     *
     * Example request:
     * GET /api/ai/ask?prompt=Hello%20world
     *
     * @param prompt The input text for the AI (required)
     * @param model The model to use for the AI (optional, default: llama3:8b)
     * @return ResponseEntity containing either:
     *         - 200 OK with the AI response (success)
     *         - 400 Bad Request with error details (invalid input)
     *         - 500 Internal Server Error (server-side error)
     */
    @GetMapping("/ask")
    public ResponseEntity<?> ask(
            @RequestParam(required = true) String prompt,
            @RequestParam(required = false, defaultValue = "llama3:8b") String model
    ) {
        try {
            // Process the prompt through the AI service with the specified model
            String response = ollamaService.askOllama(prompt, model);
            // Return successful response with HTTP 200
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle invalid input (e.g., empty prompt)
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // Handle unexpected errors
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing your request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Global exception handler for MethodArgumentTypeMismatchException.
     *
     * This method handles cases where:
     * - Required parameters are missing
     * - Parameter types don't match expected format
     * - Invalid parameter values are provided
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with HTTP 400 Bad Request and error details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Create a consistent error response format
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid parameter");
        errorResponse.put("message", "Please check your input parameters");
        return ResponseEntity.badRequest().body(errorResponse);
    }
}