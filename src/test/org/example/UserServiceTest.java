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
        // Test case 1: Valid credentials
        String username1 = "user1";
        String password1 = "Password123";
        String email1 = "user1@example.com";

        userService.signUp(username1, password1, email1);

        // Test case 2: Username already exists
        String username2 = "user1"; // Same as username1
        String password2 = "Password456";
        String email2 = "user2@example.com";

        userService.signUp(username2, password2, email2);

        // Test case 3: Invalid password length
        String username3 = "user3";
        String password3 = "short"; // Invalid password
        String email3 = "user3@example.com";

        userService.signUp(username3, password3, email3);

        // Test case 4: Valid credentials with different data
        String username4 = "user4";
        String password4 = "SecurePass789";
        String email4 = "user4@example.com";

        userService.signUp(username4, password4, email4);

        // No assertions here because signUp() directly prints to the console.
        // For better testing, you could mock the database or add a method in UserService to verify the user exists.
    }

    @Test
    void login() {
        // Arrange
        String username = "user1";
        String password = "Password123";

        // Act
        boolean result = userService.login(username, password);

        // Assert
        assertTrue(result, "Login should be successful for valid credentials.");
    }

    @Test
    void resetPassword() {
        // Arrange
        String email = "user1@example.com";
        String newPassword = "NewPassword123";

        // Act
        userService.resetPassword(email, newPassword);

        // Assert
        // You would typically check if the password was updated in the database
    }

    @Test
    void logout() {
        // Arrange
        String username = "user1";

        // Act
        userService.logout(username);

        // Assert
        // Verify that the logout time was recorded correctly in the database
    }

    @Test
    void viewLoginLogoutHistory() {
        // Arrange
        String username = "user1";

        // Act
        userService.viewLoginLogoutHistory(username);

        // Assert
        // This would involve checking the output or verifying a database query result
    }

    @Test
    void viewReadingHistory() {
        // Arrange
        String username = "user1";

        // Act
        userService.viewReadingHistory(username);

        // Assert
        // Verify that the correct reading history is retrieved, possibly by checking console output
    }

    @Test
    void viewFavorites() {
        // Arrange
        String username = "user1";

        // Act
        userService.viewFavorites(username);

        // Assert
        // Check that the favorites are correctly displayed or retrieved
    }

    @Test
    void addToReadingHistory() {
        // Arrange
        String username = "user1";
        String title = "Article 1";
        String category = "Category A";
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
        String username = "user1";
        String title = "Article 1";
        String category = "Category A";
        int rating = 4;
        boolean liked = false;
        boolean skipped = true;

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
        assertTrue(userService.isExecutorShutdown(), "Executor service should be shut down.");
    }
}