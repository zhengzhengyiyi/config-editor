package io.github.zhengzhengyiyi.api;

import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;

/**
 * A client for interacting with a local Ollama AI server.
 * Provides methods to send chat requests and check server status.
 * This client communicates with the Ollama REST API running on localhost.
 * 
 * <p><b>Usage Example:</b>
 * <pre>
 * {@code
 * AIClient client = new AIClient();
 * 
 * // Check server status first
 * client.checkServerStatus().thenAccept(available -> {
 *     if (available) {
 *         // Send chat request
 *         client.sendChatRequest("tinyllama:latest", "Hello, how are you?")
 *               .thenAccept(response -> System.out.println("AI Response: " + response));
 *     } else {
 *         System.out.println("Ollama server is not running");
 *     }
 * });
 * }
 * </pre>
 * 
 * @author zhengzhengyiyi
 * @version 1.0.0
 * @since 1.0.0
 */
public class AiClient {
    /**
     * Base URL for the Ollama server API endpoints.
     * Defaults to http://localhost:11434 which is the standard Ollama port.
     */
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    
    /**
     * HTTP client instance used for all API communications.
     */
    private final HttpClient httpClient;
    
    /**
     * Constructs a new AIClient with a default HTTP client.
     * The HTTP client is configured with default settings suitable for
     * communicating with the local Ollama server.
     */
    public AiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    /**
     * Sends a chat request to the Ollama server with the specified model and message.
     * 
     * <p>This method sends an asynchronous HTTP POST request to the Ollama chat API
     * and returns a CompletableFuture that will be completed with the AI's response.
     * 
     * <p><b>Request Flow:</b>
     * <ol>
     *   <li>Escapes the message content for JSON safety</li>
     *   <li>Builds the JSON request body with model and message</li>
     *   <li>Sends POST request to /api/chat endpoint</li>
     *   <li>Parses the response to extract the AI's content</li>
     * </ol>
     *
     * @param model the AI model to use for generating the response (e.g., "tinyllama:latest")
     * @param message the user's message to send to the AI
     * @return a CompletableFuture that will be completed with the AI's response text
     * @throws RuntimeException if the HTTP request fails or returns a non-200 status code
     * 
     * @see #escapeJson(String)
     * @see #extractContentFromResponse(String)
     */
    public CompletableFuture<String> sendChatRequest(String model, String message) {
        String requestBody = String.format(
            "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"stream\": false}",
            model, escapeJson(message)
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_BASE_URL + "/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return extractContentFromResponse(response.body());
                } else {
                    throw new RuntimeException("API request failed: " + response.statusCode() + " - " + response.body());
                }
            });
    }
    
    /**
     * Extracts the content from the Ollama API JSON response.
     * 
     * <p>Parses the JSON response string to find and extract the "content" field
     * from the AI's response. Also handles basic formatting by converting escaped
     * newlines to actual newline characters.
     *
     * @param jsonResponse the raw JSON response string from the Ollama API
     * @return the extracted content text, or an error message if parsing fails
     */
    private String extractContentFromResponse(String jsonResponse) {
        try {
            int idx = jsonResponse.indexOf("\"content\":\"");
            if (idx == -1) return "Unable to parse AI response";
            int i = idx + 11;
            StringBuilder sb = new StringBuilder();
            boolean escape = false;
            for (; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                if (escape) {
                    switch (c) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case '\\': sb.append('\\'); break;
                        case '"': sb.append('"'); break;
                        case '/': sb.append('/'); break;
                        case 'u':
                            if (i + 4 < jsonResponse.length()) {
                                String hex = jsonResponse.substring(i + 1, i + 5);
                                try {
                                    int code = Integer.parseInt(hex, 16);
                                    sb.append((char) code);
                                    i += 4;
                                } catch (Exception e) {
                                    sb.append('\\');
                                    sb.append('u');
                                }
                            } else {
                                sb.append('\\');
                                sb.append('u');
                            }
                            break;
                        default: sb.append(c); break;
                    }
                    escape = false;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error parsing response";
        }
    }
    
    /**
     * Escapes special characters in a string for safe inclusion in JSON.
     * 
     * <p>This method handles the following escape sequences:
     * <ul>
     *   <li>Backslash (\) → \\</li>
     *   <li>Double quote (") → \"</li>
     *   <li>Newline (\n) → \\n</li>
     *   <li>Carriage return (\r) → \\r</li>
     *   <li>Tab (\t) → \\t</li>
     * </ul>
     *
     * @param input the raw input string to escape
     * @return the escaped string safe for JSON inclusion
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Checks if the Ollama server is running and accessible.
     * 
     * <p>This method sends a GET request to the Ollama tags API endpoint
     * to verify that the server is responsive. The check is performed
     * asynchronously and does not block the calling thread.
     *
     * @return a CompletableFuture that will be completed with:
     *         <ul>
     *           <li>{@code true} - if the server responds with HTTP 200 status</li>
     *           <li>{@code false} - if the server is unreachable or returns an error status</li>
     *         </ul>
     */
    public CompletableFuture<Boolean> checkServerStatus() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_BASE_URL + "/api/tags"))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> response.statusCode() == 200)
            .exceptionally(ex -> false);
    }
}