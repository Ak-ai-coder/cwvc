package org.example.model;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class OllamaClient {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private String prompt;
    private final UserService userService;


    // Thread pool for handling concurrent tasks
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public OllamaClient(String username, UserService userService) {
        this.userService = userService;
        this.prompt = "Generating recommendations based on user " + username + "'s history.";
    }

    public String analyzeAndRecommendCategory(String username, List<JSONObject> allArticles) {
        Future<String> future = executorService.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                List<CategoryWeight> categoryWeights = initializeCategoryWeights(getAvailableCategories(allArticles));
                updateCategoryWeights(username, categoryWeights, connection);

                String recommendedCategory = getRecommendedCategory(categoryWeights);

                this.prompt = "User " + username + " has a history of reading articles. Based on the available categories and their interests, suggest the most suitable category from: " +
                        categoryWeights.stream().map(CategoryWeight::getCategory).collect(Collectors.joining(", ")) + ".";

                String response = sendRequest();
                System.out.println("Model Response: " + response);

                // Clean up the model's response to extract just the category name
                String cleanedCategory = cleanModelResponse(response);

                if (categoryWeights.stream().map(CategoryWeight::getCategory).collect(Collectors.toList()).contains(cleanedCategory)) {
                    return cleanedCategory;
                } else {
                    System.out.println("Falling back to the most common category: " + recommendedCategory);
                    return recommendedCategory;
                }
            }
        });

        try {
            return future.get(); // Waits for the result and returns it
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error: Unable to complete the recommendation task.";
        }
    }

    private String cleanModelResponse(String response) {
        // Extract the category from the model's response
        String[] parts = response.split(":");
        return parts.length > 0 ? parts[0].trim() : "No category found";
    }



    private List<CategoryWeight> initializeCategoryWeights(List<String> categories) {
        List<CategoryWeight> categoryWeights = new ArrayList<>();
        double initialWeight = 1.0 / categories.size();
        for (String category : categories) {
            categoryWeights.add(new CategoryWeight(category, initialWeight));
        }
        return categoryWeights;
    }

    private void updateCategoryWeights(String username, List<CategoryWeight> categoryWeights, Connection connection) throws SQLException {
        String sql = "SELECT Category, Rating, Liked,Skipped FROM ReadingHistory WHERE Username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String category = resultSet.getString("Category");
                int rating = resultSet.getInt("Rating");
                boolean liked = resultSet.getBoolean("Liked");
                boolean skipped = resultSet.getBoolean("Skipped");

                for (CategoryWeight categoryWeight : categoryWeights) {
                    if (categoryWeight.getCategory().equals(category)) {
                        double ratingMultiplier = 1.0 + (rating / 5.0);
                        double likeMultiplier = liked ? 1.5 : 1.0;
                        double skipMultiplier = skipped ? 0.5 : 1.0;
                        categoryWeight.updateWeight(ratingMultiplier * likeMultiplier *skipMultiplier);
                    }
                }
            }
        }
    }

    private String getRecommendedCategory(List<CategoryWeight> categoryWeights) {
        double totalWeight = categoryWeights.stream().mapToDouble(CategoryWeight::getWeight).sum();
        categoryWeights.forEach(categoryWeight -> categoryWeight.setWeight(categoryWeight.getWeight() / totalWeight));

        Optional<CategoryWeight> recommendedCategory = categoryWeights.stream()
                .max((c1, c2) -> Double.compare(c1.getWeight(), c2.getWeight()));

        return recommendedCategory.map(CategoryWeight::getCategory).orElse("No category found");
    }

    private List<String> getAvailableCategories(List<JSONObject> allArticles) {
        return allArticles.stream()
                .map(article -> (String) article.get("category"))
                .distinct()
                .collect(Collectors.toList());
    }

    public JSONObject getArticleForCategory(String username, String recommendedCategory, List<JSONObject> allArticles) {
        Future<JSONObject> future = executorService.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Fetch the titles of articles already read by the user
                List<String> readTitles = getReadTitles(username, connection);

                // Filter articles to match the recommended category and exclude read titles
                return allArticles.stream()
                        .filter(article -> recommendedCategory.equalsIgnoreCase((String) article.get("category")))
                        .filter(article -> !readTitles.contains((String) article.get("headline")))
                        .findFirst()
                        .orElse(null); // Return the first matching article or null if none are found
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });

        try {
            return future.get(); // Wait for the result and return it
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> getReadTitles(String username, Connection connection) throws SQLException {
        List<String> readTitles = new ArrayList<>();
        String sql = "SELECT Title FROM ReadingHistory WHERE Username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                readTitles.add(resultSet.getString("Title"));
            }
        }
        return readTitles;
    }

    public String formatArticleDetails(JSONObject article) {
        return "Recommended Article:\n" +
                "Title: " + article.get("headline") + "\n" +
                "Category: " + article.get("category") + "\n" +
                "Author: " + article.get("authors") + "\n" +
                "Date: " + article.get("date") + "\n" +
                "Link: " + article.get("link") + "\n" +
                "Description: " + article.get("short_description");
    }

    public String sendRequest() {
        Future<String> future = executorService.submit(() -> {
            try {
                URL url = new URL("http://localhost:11434/api/generate");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("model", "llama3.2");
                jsonInput.put("prompt", prompt);
                jsonInput.put("stream", false);

                String jsonInputString = jsonInput.toJSONString();

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    return "Error: Unable to connect to Ollama API, response code: " + code;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }
                conn.disconnect();

                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    return (String) jsonResponse.getOrDefault("response", "No response field found in JSON.");
                } catch (ParseException e) {
                    e.printStackTrace();
                    return "Error parsing the JSON response.";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error sending request to Ollama API.";
            }
        });

        try {
            return future.get(); // Waits for the result and returns it
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Error: Unable to complete the request.";
        }
    }
    public void shutdown(){
        executorService.shutdown();
    }
}