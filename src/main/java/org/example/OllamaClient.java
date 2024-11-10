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
            String mostReadCategory = getMostReadCategory(username, connection);
            if (mostReadCategory == null) {
                return "No category data available.";
            }
            // Logic for finding and returning an article
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Error generating recommendation.";
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
