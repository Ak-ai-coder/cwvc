package org.example;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Cli implements Runnable {
    private final UserService userService; // Instance-specific UserService
    private final AtomicBoolean active = new AtomicBoolean(true); // Tracks if the thread is active
    private List<JSONObject> jsonDataList; // Holds parsed articles data
    private String loggedInUsername; // Stores the currently logged-in username

    public Cli() {
        this.userService = new UserService(); // Initialize the UserService
    }

    public static void main(String[] args) {
        Cli cliInstance = new Cli();
        Executors.newSingleThreadExecutor().execute(cliInstance); // Run the CLI in a single-threaded executor
    }

    private static String unzip(String zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        String jsonFilePath = null;
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                    jsonFilePath = filePath;
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonFilePath;
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private static List<JSONObject> parseJsonFile(String filePath) {
        JSONParser parser = new JSONParser();
        List<JSONObject> jsonDataList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JSONObject jsonObject = (JSONObject) parser.parse(line);
                    jsonDataList.add(jsonObject);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonDataList;
    }

    public static void displayArticles(List<JSONObject> articles, String username, UserService userService) {
        Scanner scanner = new Scanner(System.in);
        for (JSONObject article : articles) {
            String title = (String) article.get("headline");
            String category = (String) article.get("category");
            String link = (String) article.get("link");
            String author = (String) article.get("authors");
            String date = (String) article.get("date");
            String description = (String) article.get("short_description");

            System.out.println("\n=== Article Details ===");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("Category: " + category);
            System.out.println("Date: " + date);
            System.out.println("Link: " + link);
            System.out.println("Description: " + description);
            System.out.println("=======================\n");

            System.out.println("Options: (1) Like (2) Skip (3) Rate (4) Next Article (5) Exit ");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline

            Integer rating = 0;
            boolean liked = false;
            boolean skipped = false;

            switch (choice) {
                case 1:
                    liked = true;
                    System.out.println("You liked this article.");
                    break;
                case 2:
                    skipped = true;
                    System.out.println("You skipped this article.");
                    break;
                case 3:
                    System.out.print("Rate this article (1-5): ");
                    rating = scanner.nextInt();
                    scanner.nextLine(); // Consume the newline
                    if (rating < 1 || rating > 5) {
                        System.out.println("Invalid rating. Must be between 1 and 5.");
                        rating = null;
                    } else {
                        System.out.println("You rated this article: " + rating);
                    }
                    break;
                case 4:
                    System.out.println("Moving to the next article...");
                    continue;
                case 5:
                    System.out.println("Exiting reading session...");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }

            userService.addToReadingHistory(username, title, category, rating, liked, skipped);
            userService.updateReadingHistory(username, title, category, rating, liked, skipped);
        }
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        // File paths for news articles
        String zipFilePath = "/Users/akshankumarsen/Downloads/News_Category_Dataset_v3.json.zip"; // Update this path
        String destDirectory = "News_Category_Dataset_v3.json"; // Update this path
        String unzippedFilePath = unzip(zipFilePath, destDirectory);
        List<JSONObject> jsonDataList = parseJsonFile(unzippedFilePath);

        while (active.get()) {
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
                        System.out.println("Login successful.");
                        loggedInUsername = loginUsername;
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
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewReadingHistory(loggedInUsername);
                    break;

                case 5:
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewLoginLogoutHistory(loggedInUsername);
                    break;

                case 6:
                    if (jsonDataList != null && !jsonDataList.isEmpty() && loggedInUsername != null) {
                        System.out.println("\nAvailable categories:");
                        Set<String> categories = jsonDataList.stream()
                                .map(article -> (String) article.get("category"))
                                .collect(Collectors.toSet());

                        int categoryIndex = 1;
                        List<String> categoryList = new ArrayList<>(categories);
                        for (String category : categoryList) {
                            System.out.println(categoryIndex + ". " + category);
                            categoryIndex++;
                        }

                        System.out.print("Select a category by entering the number: ");
                        int selectedCategoryIndex = scanner.nextInt();
                        scanner.nextLine(); // Consume the newline

                        if (selectedCategoryIndex > 0 && selectedCategoryIndex <= categoryList.size()) {
                            String selectedCategory = categoryList.get(selectedCategoryIndex - 1);

                            List<JSONObject> filteredArticles = jsonDataList.stream()
                                    .filter(article -> selectedCategory.equals(article.get("category")))
                                    .collect(Collectors.toList());

                            displayArticles(filteredArticles, loggedInUsername, userService);
                        } else {
                            System.out.println("Invalid category selection.");
                        }
                    } else {
                        System.out.println("Please log in or ensure articles are available.");
                    }
                    break;

                case 7:
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewFavorites(loggedInUsername);
                    break;

                case 8: // Generate Recommendations
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }

                    // Copy variables to ensure they are effectively final
                    final String usernameForLambda = loggedInUsername;
                    final List<JSONObject> articlesForLambda = new ArrayList<>(jsonDataList); // Make a copy to ensure it's immutable in context

                    System.out.println("Generating recommendations in the background...");
                    ExecutorService recommendationExecutor = Executors.newSingleThreadExecutor();
                    recommendationExecutor.submit(() -> {
                        try {
                            // Initialize the OllamaClient
                            OllamaClient ollamaClient = new OllamaClient("Analyzing user preferences...", userService);

                            // Get the recommended category
                            String recommendedCategory = ollamaClient.analyzeAndRecommendCategory(usernameForLambda, articlesForLambda);

                            if (recommendedCategory != null && !recommendedCategory.isEmpty()) {
                                System.out.println("Model's most recommended category: " + recommendedCategory);

                                // Fetch an article for the recommended category
                                JSONObject recommendedArticle = ollamaClient.getArticleForCategory(usernameForLambda, recommendedCategory, articlesForLambda);

                                if (recommendedArticle != null) {
                                    // Display the article details
                                    System.out.println(ollamaClient.formatArticleDetails(recommendedArticle));

                                    // Add the recommended article to the user's reading history
                                    userService.addToReadingHistory(
                                            usernameForLambda,
                                            (String) recommendedArticle.get("headline"),
                                            (String) recommendedArticle.get("category"),
                                            0, // Default rating
                                            false, // Not liked initially
                                            false // Not skipped initially
                                    );
                                    System.out.println("The recommended article has been added to your reading history.");
                                } else {
                                    System.out.println("No suitable articles found for the recommended category: " + recommendedCategory);
                                }
                            } else {
                                System.out.println("Error: No valid recommendations were generated.");
                            }

                            // Return the model's most recommended category
                            synchronized (System.out) {
                                System.out.println("Recommendation process completed for: " + usernameForLambda);
                                System.out.println("Recommended Category by Model: " + recommendedCategory);
                            }

                        } catch (Exception e) {
                            System.out.println("An error occurred while generating recommendations.");
                            e.printStackTrace();
                        }
                    });
                    recommendationExecutor.shutdown();
                    break;

                case 9:
                    if (loggedInUsername != null) {
                        userService.logout(loggedInUsername);
                        loggedInUsername = null;
                        System.out.println("Logged out successfully.");
                    } else {
                        System.out.println("No user is currently logged in.");
                    }
                    break;

                case 10:
                    System.out.println("Exiting program...");
                    active.set(false);
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }
}