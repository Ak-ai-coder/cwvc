package org.example;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.List;

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
}
