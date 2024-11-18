package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OllamaClientTest {

    @Test
    void testOllamaClientConnection() {
        UserService userService = new UserService(); // Mock or use a real UserService instance if needed
        OllamaClient ollamaClient = new OllamaClient("Test prompt for connection", userService);

        try {
            String response = ollamaClient.sendRequest(); // Adjust the method to connect to the Ollama service
            assertNotNull(response, "Response should not be null");
            assertFalse(response.startsWith("Error"), "Response should not start with 'Error'");
            System.out.println("Connection successful: " + response);
        } catch (Exception e) {
            fail("Failed to connect to the Ollama API: " + e.getMessage());
        }
    }
}