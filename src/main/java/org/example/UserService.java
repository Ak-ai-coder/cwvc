package org.example;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    // Single-threaded executor to stack and process database requests sequentially
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Sign-up method
    public void signUp(String username, String password, String email) {
        dbExecutor.submit(() -> {
            if (isUserExists(username)) {
                System.out.println("Username already exists. Please choose another.");
            } else if (password.length() < 8) {
                System.out.println("Password must be at least 8 characters long.");
            } else {
                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, username);
                    statement.setString(2, password);
                    statement.setString(3, email);


                    int rowsInserted = statement.executeUpdate();
                    if (rowsInserted > 0) {
                        System.out.println("User registered successfully.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Login method - checks if a user exists in the database


    public boolean login(String username, String password) {
        try {
            Future<Boolean> future = dbExecutor.submit(() -> {
                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String sql = "SELECT * FROM User WHERE username = ? AND password = ?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, username);
                    statement.setString(2, password);

                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        System.out.println("Login successful!");
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
            });

            // Wait for the result and return it
            return future.get(); // This blocks until the task is completed
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if a user exists in the database
    private boolean isUserExists(String username) {
        final boolean[] userExists = {false};
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT username FROM User WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                userExists[0] = resultSet.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return userExists[0];
    }

    // Reset password method
    public void resetPassword(String email, String newPassword) {
        dbExecutor.submit(() -> {
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
        });
    }

    // Update password in the database
    private void updatePassword(String email, String newPassword) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "UPDATE User SET password = ? WHERE email = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, newPassword);
                statement.setString(2, email);
                statement.executeUpdate();
                System.out.println("Password updated in the database.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Update login time
    private void updateLoginTime(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "UPDATE User SET loginTime = NOW() WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Logout method
    public void logout(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "UPDATE User SET logoutTime = NOW() WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                statement.executeUpdate();
                System.out.println("Logout successful!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // View login/logout history
    public void viewLoginLogoutHistory(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT loginTime, logoutTime FROM User WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                System.out.println("Login/Logout History:");
                if (resultSet.next()) {
                    System.out.println("Login Time: " + resultSet.getTimestamp("loginTime"));
                    System.out.println("Logout Time: " + resultSet.getTimestamp("logoutTime"));
                } else {
                    System.out.println("No history found for this user.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // View reading history for a user
    public void viewReadingHistory(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT Title, Category, Rating, Liked, Skipped FROM ReadingHistory WHERE Username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                System.out.println("Reading History:");
                boolean hasRecords = false;
                while (resultSet.next()) {
                    hasRecords = true;
                    System.out.println("Title: " + resultSet.getString("Title"));
                    System.out.println("Category: " + resultSet.getString("Category"));
                    System.out.println("Rating: " + resultSet.getInt("Rating"));
                    System.out.println("Liked: " + (resultSet.getInt("Liked") == 1 ? "Yes" : "No"));
                    System.out.println("Skipped: " + (resultSet.getInt("Skipped") == 1 ? "Yes" : "No"));
                    System.out.println("--------------------------");
                }
                if (!hasRecords) {
                    System.out.println("No reading history found for this user.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // View favorites for a user
    public void viewFavorites(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT Title, Category, Rating, Liked FROM Favorites WHERE Username = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                System.out.println("Favorite Articles:");
                boolean hasRecords = false;
                while (resultSet.next()) {
                    hasRecords = true;
                    System.out.println("Title: " + resultSet.getString("Title"));
                    System.out.println("Category: " + resultSet.getString("Category"));
                    System.out.println("Rating: " + resultSet.getInt("Rating"));
                    System.out.println("Liked: " + (resultSet.getInt("Liked") == 1 ? "Yes" : "No"));
                    System.out.println("--------------------------");
                }
                if (!hasRecords) {
                    System.out.println("No favorite articles found for this user.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    // Add to reading history
    public void addToReadingHistory(String username, String title, String category) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO ReadingHistory (Username, Title, Category, Rating, Liked, Skipped) VALUES (?, ?, ?, 0, 0, 0)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                statement.setString(2, title);
                statement.setString(3, category);
                statement.executeUpdate();
                System.out.println("Reading history entry added.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    // Update reading history
    public void updateReadingHistory(String username, String title, String category, Integer rating, Boolean liked, Boolean skipped) {
        dbExecutor.submit(() -> {
            String updateHistorySQL = "UPDATE ReadingHistory SET Rating = ?, Liked = ?, Skipped = ? WHERE Username = ? AND Title = ?";
            String insertFavoriteSQL = "INSERT INTO Favorites (Username, Title, Category, Rating, Liked) VALUES (?, ?, ?, ?, ?)";
            String updateFavoriteSQL = "UPDATE Favorites SET Rating = ?, Liked = ? WHERE Username = ? AND Title = ?";

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Step 1: Update ReadingHistory entry
                try (PreparedStatement historyStmt = connection.prepareStatement(updateHistorySQL)) {
                    historyStmt.setInt(1, (rating != null) ? rating : 0);
                    historyStmt.setInt(2, (liked != null && liked) ? 1 : 0);
                    historyStmt.setInt(3, (skipped != null && skipped) ? 1 : 0);
                    historyStmt.setString(4, username);
                    historyStmt.setString(5, title);

                    int rowsUpdated = historyStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        System.out.println("Reading history entry updated.");
                    }
                }

                // Step 2: Add to or update Favorites if criteria are met
                if (rating > 0 || (liked != null && liked)) {
                    try (PreparedStatement checkStmt = connection.prepareStatement("SELECT * FROM Favorites WHERE Username = ? AND Title = ?")) {
                        checkStmt.setString(1, username);
                        checkStmt.setString(2, title);
                        ResultSet resultSet = checkStmt.executeQuery();

                        if (resultSet.next()) {
                            try (PreparedStatement updateStmt = connection.prepareStatement(updateFavoriteSQL)) {
                                updateStmt.setInt(1, rating);
                                updateStmt.setBoolean(2, liked);
                                updateStmt.setString(3, username);
                                updateStmt.setString(4, title);
                                updateStmt.executeUpdate();
                            }
                        } else {
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
        });
    }

    // Shutdown executor service gracefully
    public void shutdown() {
        dbExecutor.shutdown();
    }
}