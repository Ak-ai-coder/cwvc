package org.example;

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
import java.util.stream.Collectors;

public class OllamaClient {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private String prompt;
    private final UserService userService;
    private JSONObject lastRecommendedArticle;

    public OllamaClient(String prompt, UserService userService) {
        this.prompt = prompt;
        this.userService = userService;
    }

    public JSONObject getRecommendedArticle() {
        return lastRecommendedArticle;
    }

    public String analyzeAndRecommendCategory(String username, List<JSONObject> allArticles) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get the most common category from the user's reading history and favorites
            String mostCommonCategory = getMostCommonCategoryFromHistoryAndFavorites(username, connection);

            // Get available categories from the dataset
            List<String> validCategories = getAvailableCategories(allArticles);
            String categoriesString = String.join(", ", validCategories);

            // Prepare the prompt for the model, including the available categories
            this.prompt = "User " + username + " has shown an interest in the " + mostCommonCategory +
                    " category. Here are the available categories: " + categoriesString +
                    ". Based on their reading history, recommend the most suitable category.";

            // Send the prompt to the model and get the response
            String response = sendRequest();

            // Log the model's raw response for debugging
            System.out.println("Model Response: " + response);

            // Ensure the response is one of the available categories
            if (validCategories.contains(response.trim())) {
                return response.trim();
            } else {
                // Log fallback to the most common category
                System.out.println("Falling back to the most common category: " + mostCommonCategory);
                return mostCommonCategory;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return "Error: Unable to connect to the database.";
        }
    }

    private String getMostCommonCategoryFromHistoryAndFavorites(String username, Connection connection) throws SQLException {
        String sql = "SELECT Category, COUNT(Category) AS CategoryCount " +
                "FROM ( " +
                "SELECT Category FROM ReadingHistory WHERE Username = ? " +
                "UNION ALL " +
                "SELECT Category FROM Favorites WHERE Username = ? " +
                ") AS CombinedCategories " +
                "GROUP BY Category " +
                "ORDER BY CategoryCount DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("Category");
            }
        }
        return null;
    }

    private List<String> getAvailableCategories(List<JSONObject> allArticles) {
        return allArticles.stream()
                .map(article -> (String) article.get("category"))
                .distinct()
                .collect(Collectors.toList());
    }

    public JSONObject getArticleForCategory(String username, String category, List<JSONObject> allArticles) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            List<String> readTitles = getReadTitles(username, connection);

            for (JSONObject article : allArticles) {
                String title = (String) article.get("headline");
                String articleCategory = (String) article.get("category");

                if (articleCategory.equals(category) && !readTitles.contains(title)) {
                    return article;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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

    public String sendRequest() throws IOException {
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
    }
}