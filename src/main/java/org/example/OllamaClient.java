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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class OllamaClient {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Method to generate a recommendation based on a user's most-read category
    public String generateRecommendationForMostReadCategory(String username, List<JSONObject> allArticles) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Find the most-read category
            String mostReadCategory = getMostReadCategory(username, connection);

            if (mostReadCategory == null) {
                return "No category data available for this user.";
            }

            // Find an article in that category that is not in the reading history or favorites
            JSONObject article = findArticleByCategory(username, mostReadCategory, allArticles, connection);

            if (article != null) {
                // Add the recommended article to the reading history
                addArticleToReadingHistory(username, article, connection);
                return "Recommended Article:\nTitle: " + article.get("headline") + "\nCategory: " + article.get("category") +
                        "\nDescription: " + article.get("short_description") + "\nDate: " + article.get("date");
            } else {
                return "No new articles available for the category: " + mostReadCategory;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Unable to connect to the database.";
        }
    }

    private String getMostReadCategory(String username, Connection connection) throws SQLException {
        String sql = "SELECT Category, COUNT(Category) AS CategoryCount FROM ReadingHistory WHERE Username = ? GROUP BY Category ORDER BY CategoryCount DESC LIMIT 1";
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
