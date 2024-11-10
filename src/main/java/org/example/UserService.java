package org.example;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    // Constructor
    public UserService() {
        // Initialize  resources
    }

    // Sign up method to add a new user to the database
    public void signUp(int userID, String username, String password, String email, String preferences) {
        if (isUserExists(username)) {
            System.out.println("Username already exists. Please choose another.");
            return;
        }

        String sql = "INSERT INTO Users (userID, username, password, email, preferences) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userID);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, email);
            statement.setString(5, preferences);

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("User registered successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Login method that checks the database for user credentials
    public boolean login(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                System.out.println("Login successful.");
                updateLoginTime(username);
                return true;
            } else {
                System.out.println("Invalid username or password.");
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if a user exists in the database
    private boolean isUserExists(String username) {
        String sql = "SELECT username FROM Users WHERE username = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            return resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update the login time for a user
    private void updateLoginTime(String username) {
        String sql = "UPDATE Users SET loginTime = NOW() WHERE username = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}