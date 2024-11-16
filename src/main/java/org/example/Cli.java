package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Cli implements Runnable {
    private static final int NUM_THREADS = 1;
    private static final UserService userService = new UserService();
    private final int threadnumber;

    public Cli(int threadnumber) {
        this.threadnumber = threadnumber;
    }

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 1; i <= NUM_THREADS; i++) {
            executorService.execute(new Cli(i));
        }
        executorService.shutdown();
    }

    // Method to unzip the file
    public static String unzip(String zipFilePath, String destDirectory) {
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

    // Helper method to extract files
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    // Method to parse the local JSON file
    public static List<JSONObject> parseJsonFile(String filePath) {
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

    // Display articles with a more user-friendly format
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

    public void run() {
        System.out.println("Starting CLI on Thread-" + threadnumber);
        Scanner scanner = new Scanner(System.in);
        int choice;
        String loggedInUsername = null;

        // Initialize NewsAPI related variables
        String zipFilePath = "/Users/akshankumarsen/Downloads/News_Category_Dataset_v3.json.zip"; // Update this path
        String destDirectory = "News_Category_Dataset_v3.json.zip"; // Update this path
        String unzippedFilePath = unzip(zipFilePath, destDirectory);
        List<JSONObject> jsonDataList = parseJsonFile(unzippedFilePath);

        do {
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
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline

            switch (choice) {
                case 1:
                    // Sign Up
                    // Consume newline
                    System.out.print("Enter Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter Password: ");
                    String password = scanner.nextLine();
                    System.out.print("Enter Email: ");
                    String email = scanner.nextLine();
                    userService.signUp(username, password, email);
                    break;

                case 2:
                    // Login
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
                    // Reset Password
                    System.out.print("Enter Email: ");
                    String resetEmail = scanner.nextLine();
                    System.out.print("Enter New Password: ");
                    String newPassword = scanner.nextLine();
                    userService.resetPassword(resetEmail, newPassword);
                    break;

                case 4:
                    // View Reading History
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewReadingHistory(loggedInUsername);
                    break;

                case 5:
                    // View Login/Logout History
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewLoginLogoutHistory(loggedInUsername);
                    break;

                case 6:
                    // News Article Reading Section with Category Selection
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

                            if (!filteredArticles.isEmpty()) {
                                displayArticles(filteredArticles, loggedInUsername, userService);
                            } else {
                                System.out.println("No articles found for the selected category.");
                            }
                        } else {
                            System.out.println("Invalid category selection.");
                        }
                    } else {
                        System.out.println("Please log in or ensure articles are available.");
                    }
                    break;

                case 7:
                    // View Favourite Articles
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }
                    userService.viewFavorites(loggedInUsername);
                    break;

                case 8:
                    if (loggedInUsername == null) {
                        System.out.println("Please log in first.");
                        break;
                    }

                    // Initialize the OllamaClient with an appropriate prompt and pass the UserService instance
                    OllamaClient ollamaClient = new OllamaClient("Analyzing user reading history for recommendations", userService);

                    // Analyze user preferences and get a recommendation
                    String recommendation = ollamaClient.analyzeAndRecommendArticle(loggedInUsername, jsonDataList);
                    if (recommendation.startsWith("Error")) {
                        System.out.println(recommendation);
                    } else {
                        System.out.println(recommendation);
                        // Retrieve the last recommended article and display details if needed
                        JSONObject lastArticle = ollamaClient.getRecommendedArticle();
                        if (lastArticle != null) {
                            System.out.println("Article added to reading history.");
                        } else {
                            System.out.println("No new article was added.");
                        }
                    }
                    break;

                case 9:
                    // Logout
                    if (loggedInUsername != null) {
                        userService.logout(loggedInUsername);
                        loggedInUsername = null;
                        System.out.println("Logged out successfully.");
                    } else {
                        System.out.println("No user is currently logged in.");
                    }
                    break;

                case 10:
                    // Exit
                    System.out.println("Exiting program...");
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        } while (choice != 10);

        scanner.close();
        System.out.println("CLI on Thread-" + threadnumber);
    }
}