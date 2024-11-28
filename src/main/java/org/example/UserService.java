package org.example;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.simple.JSONObject;

public class UserService {
    // Thread pool for asynchronous tasks
    private final ExecutorService executorService;

    public UserService() {
        this.executorService = Executors.newCachedThreadPool(); // Dynamically manages threads
    }

    public void signUp(String username, String password, String email) {
        executorService.submit(() -> {
            if (Databasehandler.isUserExists(username)) {
                System.out.println("Username already exists. Please choose another.");
            } else if (password.length() < 8) {
                System.out.println("Password must be at least 8 characters long.");
            } else {
                Databasehandler.signUp(username, password, email);
            }
        });
    }

    public boolean login(String username, String password) {
        // Check if the user is already logged in
        if (Databasehandler.isUserLoggedIn(username)) {
            System.out.println("User is already logged in. Please log out from the other session first.");
            return false;
        }

        // Attempt to log in the user
        boolean success = Databasehandler.login(username, password);

        if (success) {
            System.out.println("Login successful! Welcome, " + username + "!");
        } else {
            System.out.println("Invalid username or password. Please try again.");
        }

        return success;
    }
    public void logoutAllUsers() {
        Databasehandler.logoutAllUsers();
    }

    public void resetPassword(String email, String newPassword) {
        executorService.submit(() -> {
            if (newPassword.length() < 8) {
                System.out.println("New password must be at least 8 characters long.");
            } else {
                Databasehandler.resetPassword(email, newPassword);
            }
        });
    }

    public void viewReadingHistory(String username) {
        executorService.submit(() -> Databasehandler.viewReadingHistory(username));
    }

    public void viewFavorites(String username) {
        executorService.submit(() -> Databasehandler.viewFavorites(username));
    }

    public void updateReadingHistory(String username, String title, String category, int rating, boolean liked, boolean skipped) {
        executorService.submit(() -> Databasehandler.updateReadingHistory(username, title, category, rating, liked, skipped));
    }

    public void viewLoginLogoutHistory(String username) {
        executorService.submit(() -> Databasehandler.viewLoginLogoutHistory(username));
    }

    public void logout(String username) {
        executorService.submit(() -> Databasehandler.logout(username));
    }

    // Generate recommendations asynchronously
    // This method handles generating recommendations asynchronously
    public void generateRecommendations(String username, List<JSONObject> allArticles) {
        executorService.submit(() -> {
            try {
                // Initialize the OllamaClient to handle recommendation
                OllamaClient ollamaClient = new OllamaClient("User recommendation prompt", this);

                // Get the recommended category for the user
                String recommendedCategory = ollamaClient.analyzeAndRecommendCategory(username, allArticles);

                // Fetch an article from the recommended category
                JSONObject recommendedArticle = ollamaClient.getArticleForCategory(username, recommendedCategory, allArticles);

                if (recommendedArticle != null) {
                    // Display the article details
                    System.out.println(ollamaClient.formatArticleDetails(recommendedArticle));

                    // Add the recommended article to the user's reading history
                    addToReadingHistory(username, (String) recommendedArticle.get("headline"), recommendedCategory, 0, false, false);
                } else {
                    System.out.println("No suitable articles found for the recommended category: " + recommendedCategory);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("An error occurred while generating recommendations.");
            }
        });
    }


    public void addToReadingHistory(String username, String title, String category, Integer rating, boolean liked, boolean skipped) {
        executorService.submit(() -> {
            try {
                Databasehandler.addToReadingHistory(username, title, category, rating, liked, skipped);
                System.out.println("Reading history updated successfully for user: " + username);
            } catch (Exception e) {
                System.err.println("Error while updating reading history for user: " + username);
                e.printStackTrace();
            }
        });
    }
    public void updateReadingHistory(String username, String title, String category, Integer rating, boolean liked, boolean skipped) {
        executorService.submit(() -> {
            try {
                // Call the database handler method
                Databasehandler.updateReadingHistory(username, title, category, rating, liked, skipped);
                System.out.println("Reading history updated successfully for user: " + username);
            } catch (Exception e) {
                System.err.println("Error while updating reading history for user: " + username);
                e.printStackTrace();
            }
        });
    }

    // Shutdown the executor service gracefully
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown as tasks are not completed in time.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Shutdown interrupted. Forcing shutdown.");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    // Check if executor is shutdown
    public boolean isExecutorShutdown() {
        return executorService.isShutdown();
    }

    public boolean isExecutorTerminated() {
        return executorService.isTerminated();
    }

    // Functional interface for recommendation handling
    public interface RecommendationHandler {
        String analyzeAndRecommendCategory(String username, List<JSONObject> articles);

        JSONObject getArticleForCategory(String username, String category, List<JSONObject> articles);

        String formatArticleDetails(JSONObject article);
    }
}