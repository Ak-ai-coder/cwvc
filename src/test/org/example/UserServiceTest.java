package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(); // Initialize UserService before each test
    }

    @Test
    void signUp() {
        // Arrange
        String username = "testUser";
        String password = "TestPass123";
        String email = "testuser@example.com";

        // Act
        userService.signUp(username, password, email);

        // Assert
        // Check the console output or add a method to verify the user was added
        // This could be a database check or a mock verification
    }

    @Test
    void login() {
        // Arrange
        String username = "testUser";
        String password = "TestPass123";

        // Act
        boolean result = userService.login(username, password);

        // Assert
        assertTrue(result, "Login should be successful for valid credentials.");
    }

    @Test
    void resetPassword() {
        // Arrange
        String email = "testuser@example.com";
        String newPassword = "NewPass123";

        // Act
        userService.resetPassword(email, newPassword);

        // Assert
        // You would typically check if the password was updated in the database
    }

    @Test
    void logout() {
        // Arrange
        String username = "testUser";

        // Act
        userService.logout(username);

        // Assert
        // Verify that the logout time was recorded correctly in the database
    }

    @Test
    void viewLoginLogoutHistory() {
        // Arrange
        String username = "testUser";

        // Act
        userService.viewLoginLogoutHistory(username);

        // Assert
        // This would involve checking the output or verifying a database query result
    }

    @Test
    void viewReadingHistory() {
        // Arrange
        String username = "testUser";

        // Act
        userService.viewReadingHistory(username);

        // Assert
        // Verify that the correct reading history is retrieved, possibly by checking console output
    }

    @Test
    void viewFavorites() {
        // Arrange
        String username = "testUser";

        // Act
        userService.viewFavorites(username);

        // Assert
        // Check that the favorites are correctly displayed or retrieved
    }

    @Test
    void addToReadingHistory() {
        // Arrange
        String username = "testUser";
        String title = "Test Article";
        String category = "Test Category";
        int rating = 5;
        boolean liked = true;
        boolean skipped = false;

        // Act
        userService.addToReadingHistory(username, title, category, rating, liked, skipped);

        // Assert
        // Confirm that the reading history entry is added properly
    }

    @Test
    void updateReadingHistory() {
        // Arrange
        String username = "testUser";
        String title = "Test Article";
        String category = "Test Category";
        int rating = 5;
        boolean liked = true;
        boolean skipped = false;

        // Act
        userService.updateReadingHistory(username, title, category, rating, liked, skipped);

        // Assert
        // Verify that the reading history is updated as expected
    }

    @Test
    void shutdown() {
        // Act
        userService.shutdown();

        // Assert
        // Confirm that the executor service has shut down properly
    }
}