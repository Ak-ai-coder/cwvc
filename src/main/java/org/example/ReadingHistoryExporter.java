package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class ReadingHistoryExporter {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public static void exportReadingHistoryToFile(String username, String filePath) {
        String sql = "SELECT Title, Category, Rating, Liked FROM ReadingHistory WHERE Username = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String title = resultSet.getString("Title");
                String category = resultSet.getString("Category");
                int rating = resultSet.getInt("Rating");
                boolean liked = resultSet.getInt("Liked") == 1;

                writer.write("Title: " + title + ", Category: " + category + ", Rating: " + rating + ", Liked: " + (liked ? "Yes" : "No"));
                writer.newLine();
            }

            System.out.println("Reading history exported to " + filePath);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
