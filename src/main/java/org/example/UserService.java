package org.example;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    // Sign-up method
    public void signUp( String username, String password, String email, String preferences) {
        if (isUserExists(username)) {
            System.out.println("Username already exists. Please choose another.");
        } else if (password.length() < 8) {
            System.out.println("Password must be at least 8 characters long.");
        } else {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
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
            String sql = "SELECT * FROM User WHERE username = ? AND password = ?";
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
        String sql = "SELECT username FROM User WHERE username = ?";
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
            String sql = "SELECT * FROM User WHERE email = ?";
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
            String sql = "UPDATE User SET password = ? WHERE email = ?";
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
            String sql = "UPDATE User SET loginTime = NOW() WHERE username = ?";
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
            String sql = "UPDATE User SET logoutTime = NOW() WHERE username = ?";
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
    public void updateReadingHistory(String username, String title, String category, Integer rating, Boolean liked, Boolean skipped) {
            String updateHistorySQL = "UPDATE ReadingHistory SET Rating = ?, Liked = ?, Skipped = ? WHERE Username = ? AND Title = ?";
            String insertFavoriteSQL = "INSERT INTO Favorites (Username, Title, Category, Rating, Liked) VALUES (?, ?, ?, ?, ?)";
            String updateFavoriteSQL = "UPDATE Favorites SET rating = ?, liked = ? WHERE username = ? AND article_title = ?";

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Step 1: Update ReadingHistory entry
                try (PreparedStatement historyStmt = connection.prepareStatement(updateHistorySQL)) {
                    historyStmt.setInt(1, (rating != null) ? rating : 0);
                    historyStmt.setInt(2, liked ? 1 : 0);
                    historyStmt.setInt(3, skipped ? 1 : 0);
                    historyStmt.setString(4, username);
                    historyStmt.setString(5, title);

                    int rowsUpdated = historyStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        System.out.println("Reading history entry updated.");
                    }
                }

                // Step 2: Add to or update UserRecommendations if criteria are met
                if (rating > 0 || liked) {
                    // Check if the article is already in UserRecommendations
                    try (PreparedStatement checkStmt = connection.prepareStatement("SELECT * FROM Favorites WHERE username = ? AND title = ?")) {
                        checkStmt.setString(1, username);
                        checkStmt.setString(2, title);
                        ResultSet resultSet = checkStmt.executeQuery();

                        if (resultSet.next()) {
                            // If entry exists, update it
                            try (PreparedStatement updateStmt = connection.prepareStatement(updateFavoriteSQL)) {
                                updateStmt.setInt(1, rating);
                                updateStmt.setBoolean(2, liked);
                                updateStmt.setString(3, username);
                                updateStmt.setString(4, title);
                                updateStmt.executeUpdate();
                            }
                        } else {
                            // If entry does not exist, insert it
                            try (PreparedStatement insertStmt = connection.prepareStatement(insertFavoriteSQL)) {
                                insertStmt.setString(1, username);
                                insertStmt.setString(2, title);
                                insertStmt.setString(3, category);
                                insertStmt.setInt(4, rating);
                                insertStmt.setBoolean(5, liked);
                                insertStmt.executeUpdate();
                            }
                        }
                    }
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
    public void viewFavorites(String username) {
            String sql = "SELECT title, category, rating, liked FROM Favorites WHERE username = ?";
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();

                System.out.println("Favorite Articles:");
                boolean hasRecords = false;

                while (resultSet.next()) {
                    hasRecords = true;
                    String title = resultSet.getString("title");
                    String category = resultSet.getString("category");
                    int rating = resultSet.getInt("rating");
                    boolean liked = resultSet.getInt("liked") == 1;

                    System.out.println("Title: " + title);
                    System.out.println("Category: " + category);
                    System.out.println("Rating: " + rating);
                    System.out.println("Liked: " + (liked ? "Yes" : "No"));
                    System.out.println("--------------------------");
                }

                if (!hasRecords) {
                    System.out.println("No favorite articles found for this user.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}