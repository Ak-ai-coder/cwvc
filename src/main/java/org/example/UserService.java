package org.example;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password

    // Single-threaded executor to process database requests sequentially
    private static  ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    public UserService(){
        this.dbExecutor = Executors.newSingleThreadExecutor();
    }
    // Sign-up method
    public void signUp(String username, String password, String email) {
        dbExecutor.submit(() -> {
            try {
                if (isUserExists(username)) {
                    System.out.println("Username already exists. Please choose another.");
                } else if (password.length() < 8) {
                    System.out.println("Password must be at least 8 characters long.");
                } else {
                    try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        String sql = "INSERT INTO User (username, password, email) VALUES (?, ?, ?)";
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.setString(1, username);
                            statement.setString(2, password);
                            statement.setString(3, email);

                            int rowsInserted = statement.executeUpdate();
                            if (rowsInserted > 0) {
                                System.out.println("User registered successfully.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Login method
    public boolean login(String username, String password) {
        try {
            Future<Boolean> future = dbExecutor.submit(() -> {
                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String sql = "SELECT * FROM User WHERE username = ? AND password = ?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
                    }
                }
            });
            return future.get(); // Wait for the result and return it
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if a user exists in the database
    private boolean isUserExists(String username) {
        try {
            Future<Boolean> future = dbExecutor.submit(() -> {
                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String sql = "SELECT username FROM User WHERE username = ?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, username);
                        ResultSet resultSet = statement.executeQuery();
                        return resultSet.next();
                    }
                }
            });
            return future.get(); // Wait for the result
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reset password method
    public void resetPassword(String email, String newPassword) {
        dbExecutor.submit(() -> {
            try {
                if (newPassword.length() < 8) {
                    System.out.println("New password must be at least 8 characters long.");
                    return;
                }

                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String sql = "SELECT * FROM User WHERE email = ?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, email);

                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            updatePassword(email, newPassword);
                            System.out.println("Password reset successfully.");
                        } else {
                            System.out.println("No user found with the provided email.");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Update password in the database
    private void updatePassword(String email, String newPassword) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "UPDATE User SET password = ? WHERE email = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, newPassword);
                    statement.setString(2, email);
                    statement.executeUpdate();
                    System.out.println("Password updated in the database.");
                }
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
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    statement.executeUpdate();
                }
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
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    statement.executeUpdate();
                    System.out.println("Logout successful!");
                }
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
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    ResultSet resultSet = statement.executeQuery();
                    System.out.println("Login/Logout History:");
                    if (resultSet.next()) {
                        System.out.println("Login Time: " + resultSet.getTimestamp("loginTime"));
                        System.out.println("Logout Time: " + resultSet.getTimestamp("logoutTime"));
                    } else {
                        System.out.println("No history found for this user.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // View reading history
    public void viewReadingHistory(String username) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT Title, Category, Rating, Liked, Skipped FROM ReadingHistory WHERE Username = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    ResultSet resultSet = statement.executeQuery();
                    System.out.println("Reading History:");
                    while (resultSet.next()) {
                        System.out.println("Title: " + resultSet.getString("Title"));
                        System.out.println("Category: " + resultSet.getString("Category"));
                        System.out.println("Rating: " + resultSet.getInt("Rating"));
                        System.out.println("Liked: " + (resultSet.getInt("Liked") == 1 ? "Yes" : "No"));
                        System.out.println("Skipped: " + (resultSet.getInt("Skipped") == 1 ? "Yes" : "No"));
                        System.out.println("--------------------------");
                    }
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
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    ResultSet resultSet = statement.executeQuery();

                    System.out.println("\n=== Favorite Articles ===");
                    boolean hasFavorites = false;
                    while (resultSet.next()) {
                        hasFavorites = true;
                        System.out.println("Title: " + resultSet.getString("Title"));
                        System.out.println("Category: " + resultSet.getString("Category"));
                        System.out.println("Rating: " + resultSet.getInt("Rating"));
                        System.out.println("Liked: " + (resultSet.getBoolean("Liked") ? "Yes" : "No"));
                        System.out.println("--------------------------");
                    }
                    if (!hasFavorites) {
                        System.out.println("No favorite articles found for this user.");
                    }
                }
            } catch (SQLException e) {
                System.err.println("An error occurred while fetching favorites.");
                e.printStackTrace();
            }
        });
    }

    // Add to reading history
    public void addToReadingHistory(String username, String title, String category, int rating, boolean liked, boolean skipped) {
        dbExecutor.submit(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO ReadingHistory (Username, Title, Category, Rating, Liked, Skipped) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username);
                    statement.setString(2, title);
                    statement.setString(3, category);
                    statement.setInt(4, rating);
                    statement.setBoolean(5, liked);
                    statement.setBoolean(6, skipped);
                    statement.executeUpdate();
                    System.out.println("Reading history entry added.");
                }
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
                connection.setAutoCommit(false);

                try {
                    // Update ReadingHistory entry
                    try (PreparedStatement historyStmt = connection.prepareStatement(updateHistorySQL)) {
                        historyStmt.setInt(1, rating != null ? rating : 0);
                        historyStmt.setBoolean(2, liked != null && liked);
                        historyStmt.setBoolean(3, skipped != null && skipped);
                        historyStmt.setString(4, username);
                        historyStmt.setString(5, title);
                        historyStmt.executeUpdate();
                    }

                    // Add or update Favorites
                    if ((rating != null && rating > 0) || (liked != null && liked)) {
                        try (PreparedStatement checkStmt = connection.prepareStatement("SELECT * FROM Favorites WHERE Username = ? AND Title = ?")) {
                            checkStmt.setString(1, username);
                            checkStmt.setString(2, title);
                            ResultSet resultSet = checkStmt.executeQuery();

                            if (resultSet.next()) {
                                try (PreparedStatement updateStmt = connection.prepareStatement(updateFavoriteSQL)) {
                                    updateStmt.setInt(1, rating != null ? rating : 0);
                                    updateStmt.setBoolean(2, liked != null && liked);
                                    updateStmt.setString(3, username);
                                    updateStmt.setString(4, title);
                                    updateStmt.executeUpdate();
                                }
                            } else {
                                try (PreparedStatement insertStmt = connection.prepareStatement(insertFavoriteSQL)) {
                                    insertStmt.setString(1, username);
                                    insertStmt.setString(2, title);
                                    insertStmt.setString(3, category);
                                    insertStmt.setInt(4, rating != null ? rating : 0);
                                    insertStmt.setBoolean(5, liked != null && liked);
                                    insertStmt.executeUpdate();
                                }
                            }
                        }
                    }

                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    e.printStackTrace();
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Shutdown executor service gracefully
    public void shutdown() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown as tasks are not completed in time.");
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Shutdown interrupted. Forcing shutdown.");
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper to check if executor service is shut down
    public boolean isExecutorShutdown() {
        return dbExecutor.isShutdown();
    }

    public boolean isExecutorTerminated() {
        return dbExecutor.isTerminated();
    }
}