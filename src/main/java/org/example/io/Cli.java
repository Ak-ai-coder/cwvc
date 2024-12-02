package org.example.io;
import org.example.json.Articles;
import org.example.model.*;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Cli implements Runnable {
    private final UserService userService; // Handles user operations
    private final Articles articles; // Handles dataset operations
    private String loggedInUsername; // Stores the currently logged-in username
    private final ExecutorService recommendationExecutor; // For async recommendations

    public Cli(String zipFilePath, String destDirectory) {
        this.userService = new UserService();
        this.articles = new Articles(zipFilePath, destDirectory);
        this.recommendationExecutor = Executors.newSingleThreadExecutor();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application is shutting down. Logging out all users...");
            Databasehandler.logoutAllUsers();
            try {
                Thread.sleep(1000); // Delay to ensure query execution completes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }



    public static void main(String[] args) {
        String zipFilePath = "/Users/akshankumarsen/Downloads/News_Category_Dataset_v3.json.zip";
        String destDirectory = "News_Category_Dataset_v3.json";
        Cli cli = new Cli(zipFilePath, destDirectory);
        new Thread(cli).start(); // Start the CLI application
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== User Service Menu ===");
            System.out.println("1. Sign Up");
            System.out.println("2. Login");
            System.out.println("3. Reset Password");
            System.out.println("4. View Reading History");
            System.out.println("5. View Login/Logout History");
            System.out.println("6. Read News Articles");
            System.out.println("7. View Favourite Articles");
            System.out.println("8. Generate Recommendations");
            System.out.println("9. Logout");
            System.out.println("10. Exit");

            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String password = scanner.nextLine();
                    System.out.print("Enter Email: ");
                    String email = scanner.nextLine();
                    userService.signUp(username, password, email);
                    break;

                case 2:
                    System.out.print("Enter Username: ");
                    String loginUsername = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String loginPassword = scanner.nextLine();


                    boolean loginSuccess = userService.login(loginUsername, loginPassword);

                    if (loginSuccess) {
                        loggedInUsername = loginUsername;
                        System.out.println("Welcome, " + loggedInUsername + "!");
                    } else {
                        System.out.println("Login failed. Please try again.");
                    }
                    break;

                case 3:
                    System.out.print("Enter Email: ");
                    String resetEmail = scanner.nextLine();
                    System.out.print("Enter New Password: ");
                    String newPassword = scanner.nextLine();
                    userService.resetPassword(resetEmail, newPassword);
                    break;

                case 4:
                    if (loggedInUsername != null) {
                        userService.viewReadingHistory(loggedInUsername);
                    } else {
                        System.out.println("Please log in first.");
                    }
                    break;

                case 5:
                    if (loggedInUsername != null) {
                        userService.viewLoginLogoutHistory(loggedInUsername);
                    } else {
                        System.out.println("Please log in first.");
                    }
                    break;

                case 6:
                    if (!articles.isArticlesLoaded()) {
                        System.out.println("Articles not loaded. Please try again later.");
                        break;
                    }

                    System.out.println("Available Categories:");
                    List<String> categories = articles.getCategories();
                    for (int i = 0; i < categories.size(); i++) {
                        System.out.println((i + 1) + ". " + categories.get(i));
                    }

                    System.out.print("Select a category by entering the number: ");
                    int categoryIndex = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    if (categoryIndex > 0 && categoryIndex <= categories.size()) {
                        String selectedCategory = categories.get(categoryIndex - 1);
                        List<JSONObject> filteredArticles = articles.getArticlesByCategory(selectedCategory);
                        Articles.displayArticles(filteredArticles, loggedInUsername, userService);
                    } else {
                        System.out.println("Invalid category selection.");
                    }
                    break;

                case 7:
                    if (loggedInUsername != null) {
                        userService.viewFavorites(loggedInUsername);
                    } else {
                        System.out.println("Please log in first.");
                    }
                    break;

                case 8: // Generate Recommendations
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }

                    if (!articles.isArticlesLoaded()) {
                        System.out.println("Articles not loaded. Please try again later.");
                        break;
                    }

                    // Call the UserService to generate recommendations
                    System.out.println("Generating recommendations in the background...");
                    userService.generateRecommendations(loggedInUsername, articles.getJsonDataList());
                    break;

                case 9:
                    if (loggedInUsername != null) {
                        userService.logout(loggedInUsername);
                        loggedInUsername = null;
                    } else {
                        System.out.println("No user is currently logged in.");
                    }
                    break;

                case 10:
                    System.out.println("Exiting program...");

                    // Log out all users
                    try {
                        System.out.println("Logging out all users...");
                        Databasehandler.logoutAllUsers();
                    } catch (Exception e) {
                        System.err.println("Error while logging out users: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // Shut down the executor for async tasks
                    recommendationExecutor.shutdown();
                    try {
                        if (!recommendationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                            System.out.println("Forcing shutdown of async tasks...");
                            recommendationExecutor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Error during executor shutdown. Forcing shutdown...");
                        recommendationExecutor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }

                    // Exit the application
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}