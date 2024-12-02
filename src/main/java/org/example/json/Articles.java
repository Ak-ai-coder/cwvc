package org.example.json;
import org.example.model.*;
import org.example.io.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Articles {
    private final String zipFilePath; // Path to the dataset ZIP file
    private final String destDirectory; // Directory where the dataset will be extracted
    private List<JSONObject> jsonDataList; // Parsed list of articles

    public Articles(String zipFilePath, String destDirectory) {
        this.zipFilePath = zipFilePath;
        this.destDirectory = destDirectory;
        this.jsonDataList = new ArrayList<>();
        loadDataset(); // Initial load
    }

    public void loadDataset() {
        try {
            String unzippedFilePath = unzip(zipFilePath, destDirectory);
            this.jsonDataList = parseJsonFile(unzippedFilePath);
            System.out.println("Articles dataset loaded successfully.");
        } catch (Exception e) {
            System.out.println("Error loading dataset.");
            e.printStackTrace();
        }
    }


    public boolean isArticlesLoaded() {
        return jsonDataList != null && !jsonDataList.isEmpty();
    }


    public List<String> getCategories() {
        Set<String> categories = new HashSet<>();
        for (JSONObject article : jsonDataList) {
            categories.add((String) article.get("category"));
        }
        return new ArrayList<>(categories);
    }
    public List<JSONObject> getJsonDataList() {
        return jsonDataList;
    }


    public List<JSONObject> getArticlesByCategory(String category) {
        List<JSONObject> filteredArticles = new ArrayList<>();
        for (JSONObject article : jsonDataList) {
            if (category.equals(article.get("category"))) {
                filteredArticles.add(article);
            }
        }
        return filteredArticles;
    }

    public static void displayArticles(List<JSONObject> articles, String username, UserService userService) {
        Scanner scanner = new Scanner(System.in);

        for (JSONObject article : articles) {
            // Extracting all the relevant details from the article JSON
            String title = (String) article.get("headline");
            String category = (String) article.get("category");
            String link = (String) article.get("link");
            String author = (String) article.get("authors");
            String date = (String) article.get("date");
            String description = (String) article.get("short_description");
            String content = (String) article.get("content");  // Add more fields as per dataset

            // Display the article details
            System.out.println("\n=== Article Details ===");
            System.out.println("Title: " + title);
            System.out.println("Category: " + category);
            System.out.println("Date: " + date);
            System.out.println("Author(s): " + author);
            System.out.println("Link: " + link);
            System.out.println("Description: " + description);
            System.out.println("=======================\n");

            // Offer user interaction options
            System.out.println("Options: (1) Like (2) Skip (3) Rate (4) Next Article (5) Exit ");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

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
                    scanner.nextLine(); // Consume newline
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

            // Add the article's user interaction to the reading history
            userService.addToReadingHistory(username, title, category, rating, liked, skipped);
            userService.updateReadingHistory(username, title, category, rating, liked, skipped);
        }
    }

    private static String unzip(String zipFilePath, String destDirectory) throws IOException {
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
        List<JSONObject> jsonDataList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    jsonDataList.add((JSONObject) parser.parse(line));
                } catch (ParseException e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON file.");
            e.printStackTrace();
        }
        return jsonDataList;
    }
}