package org.example;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    // Sign-up method
    public void signUp(int userID, String username, String password, String email, String preferences) {
        if (isUserExists(username)) {
            System.out.println("Username already exists. Please choose another.");
        } else if (password.length() < 8) {
            System.out.println("Password must be at least 8 characters long.");
        } else {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO Users (userID, username, password, email) VALUES (?, ?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setInt(1, userID);
                statement.setString(2, username);
                statement.setString(3, password);
                statement.setString(4, email);


                int rowsInserted = statement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("User registered successfully.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Login method - checks if a user exists in the database
    public boolean login(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                System.out.println("Login successful!");
                updateLoginTime(username); // Update login time
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
            return resultSet.next(); // Returns true if a user is found
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reset password method
    public void resetPassword(String email, String newPassword) {
        if (newPassword.length() < 8) {
            System.out.println("New password must be at least 8 characters long.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM Users WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, email);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                updatePassword(email, newPassword);
                System.out.println("Password reset successfully.");
            } else {
                System.out.println("No user found with the provided email.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to update the password for the user in the database
    private void updatePassword(String email, String newPassword) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE Users SET password = ? WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newPassword);
            statement.setString(2, email);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Password updated in the database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update login time - track only on actual login
    private void updateLoginTime(String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE Users SET loginTime = NOW() WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Logout method - updates logout time using username
    public void logout(String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE Users SET logoutTime = NOW() WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.executeUpdate();
            System.out.println("Logout successful!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void viewLoginLogoutHistory(String username) {
            String sql = "SELECT loginTime, logoutTime FROM User WHERE username = ?";
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, username);  // Bind the username instead of userID
                System.out.println("Checking history for username: " + username);  // Debugging output

                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    Timestamp loginTime = resultSet.getTimestamp("loginTime");
                    Timestamp logoutTime = resultSet.getTimestamp("logoutTime");

                    System.out.println("Login Time: " + (loginTime != null ? loginTime : "Not available"));
                    System.out.println("Logout Time: " + (logoutTime != null ? logoutTime : "Not available"));
                } else {
                    System.out.println("User not found.");  // Will display if no results are found
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
    public void addToReadingHistory(String username, String title, String category) {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO ReadingHistory (Username, Title, Category, Rating, Liked, Skipped) VALUES (?, ?, ?, 0, 0, 0)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                statement.setString(2, title);
                statement.setString(3, category);

                int rowsInserted = statement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Reading history entry added.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
    // View reading history for a user
    public void viewReadingHistory(String username) {
        String sql = "SELECT Title, Category, Rating, Liked, Skipped FROM ReadingHistory WHERE Username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Reading History:");
            boolean hasRecords = false;
            while (resultSet.next()) {
                hasRecords = true;
                String title = resultSet.getString("Title");
                String category = resultSet.getString("Category");
                int rating = resultSet.getInt("Rating");
                boolean liked = resultSet.getInt("Liked") == 1;
                boolean skipped = resultSet.getInt("Skipped") == 1;

                System.out.println("Title: " + title);
                System.out.println("Category: " + category);
                System.out.println("Rating: " + rating);
                System.out.println("Liked: " + (liked ? "Yes" : "No"));
                System.out.println("Skipped: " + (skipped ? "Yes" : "No"));
                System.out.println("--------------------------");
            }
            if (!hasRecords) {
                System.out.println("No reading history found for this user.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}