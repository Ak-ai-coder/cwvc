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

public class OllamaClient {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private String prompt;

    public OllamaClient(String prompt) {
        this.prompt = prompt;
    }

    // Method to analyze user preferences and recommend an article


    private String getMostLikedCategory(String username, Connection connection) throws SQLException {
        String sql = "SELECT Category, COUNT(Category) AS CategoryCount " +
                "FROM ReadingHistory " +
                "WHERE Username = ? " +
                "GROUP BY Category " +
                "ORDER BY CategoryCount DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("Category");
            }
        }
        return null;
    }

    private JSONObject findArticleByCategory(String username, String category, List<JSONObject> allArticles, Connection connection) throws SQLException {
        List<String> readTitles = new ArrayList<>();
        String sql = "SELECT Title FROM ReadingHistory WHERE Username = ? UNION SELECT Title FROM Favorites WHERE Username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                readTitles.add(resultSet.getString("Title"));
            }
        }

        for (JSONObject article : allArticles) {
            String title = (String) article.get("headline");
            String articleCategory = (String) article.get("category");
            if (articleCategory.equals(category) && !readTitles.contains(title)) {
                return article;
            }
        }
        return null;
    }

    private void addArticleToReadingHistory(String username, JSONObject article, Connection connection) throws SQLException {
        String sql = "INSERT INTO ReadingHistory (Username, Title, Category, Rating, Liked, Skipped) VALUES (?, ?, ?, 0, 0, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, (String) article.get("headline"));
            statement.setString(3, (String) article.get("category"));
            statement.executeUpdate();
            System.out.println("Article added to reading history.");
        }
    }
    public String analyzeAndRecommendArticle(String username, List<JSONObject> allArticles) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get the most-liked category from the user's history and favorites
            String mostLikedCategory = getMostLikedCategory(username, connection);

            if (mostLikedCategory == null) {
                return "No category data available for this user.";
            }

            // Prepare a prompt for the Ollama model with user history data
            String userPrompt = "User " + username + " has a strong interest in the following category: " + mostLikedCategory +
                    ". Based on this interest, recommend an article that would suit their reading preferences.";
            this.prompt = userPrompt;  // Set the prompt for the Ollama model

            // Use the model to process the prompt and get a response
            String modelResponse = sendRequest();
            if (modelResponse.startsWith("Error")) {
                return modelResponse;
            }

            // Find an article from the most-liked category that is not in the reading history or favorites
            JSONObject article = findArticleByCategory(username, mostLikedCategory, allArticles, connection);

            if (article != null) {
                // Add the recommended article to the reading history
                addArticleToReadingHistory(username, article, connection);
                return "Recommended Article from Model:\n" +
                        "Title: " + article.get("headline") + "\n" +
                        "Category: " + article.get("category") + "\n" +
                        "Author: " + article.get("authors") + "\n" +
                        "Date: " + article.get("date") + "\n" +
                        "Link: " + article.get("link") + "\n" +
                        "Description: " + article.get("short_description") + "\n\n" +
                        "Model's Suggestion: " + modelResponse;
            } else {
                return "No new articles available for the category: " + mostLikedCategory;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Unable to connect to the database.";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatArticleDetails(JSONObject article) {
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