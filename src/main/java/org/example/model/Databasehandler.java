package org.example.model;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Databasehandler {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Single-threaded executor for stacking database requests
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private static final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    public static void executeDatabaseTask(Runnable task) {
        taskQueue.add(task);
        dbExecutor.submit(() -> {
            try {
                Runnable dbTask = taskQueue.take();
                dbTask.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void shutdown() {
        dbExecutor.shutdown();
    }

    public static boolean isUserExists(String username) {
        String query = "SELECT username FROM User WHERE username = ?";
        return executeQueryWithResult(query, statement -> statement.setString(1, username), ResultSet::next);
    }

    public static void signUp(String username, String password, String email) {
        String query = "INSERT INTO User (username, password, email) VALUES (?, ?, ?)";
        executeUpdate(query, statement -> {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, email);
        }, rowsAffected -> {
            if (rowsAffected > 0) {
                System.out.println("User registered successfully.");
            }
        });
    }
    public static boolean login(String username, String password) {
        if (isUserLoggedIn(username)) {
            System.out.println("User is already logged in.");
            return false;
        }

        String queryCheck = "SELECT * FROM User WHERE username = ? AND password = ?";
        String queryUpdate = "UPDATE User SET isLoggedIn = TRUE, loginTime = NOW() WHERE username = ?";

        boolean loginSuccessful = executeQueryWithResult(queryCheck, statement -> {
            statement.setString(1, username);
            statement.setString(2, password);
        }, resultSet -> {
            if (resultSet.next()) {
                // Update login status and time explicitly
                updateLoginStatus(username, true);
                updateLoginTime(username);
                return true;
            }
            return false; // Credentials are invalid
        });

        if (loginSuccessful) {
            System.out.println("User login status and login time updated successfully.");
        } else {
            System.out.println("Failed to log in the user.");
        }

        return loginSuccessful;
    }

    // Helper method to check if a user is already logged in
    public static boolean isUserLoggedIn(String username) {
        String query = "SELECT isLoggedIn FROM User WHERE username = ?";
        return executeQueryWithResult(query, statement -> {
            statement.setString(1, username);
        }, resultSet -> {
            if (resultSet.next()) {
                return resultSet.getBoolean("isLoggedIn");
            }
            return false;
        });
    }
    public static boolean logoutAllUsers() {
        String query = "UPDATE User SET isLoggedIn = FALSE WHERE isLoggedIn = TRUE";
        try {
            executeUpdate(query, statement -> {
                // No parameters to set in this query
            }, rowsAffected -> {
                if (rowsAffected > 0) {
                    System.out.println("All logged-in users successfully logged out.");
                } else {
                    System.out.println("No users were logged out.");
                }
            });
            return true; // Indicate successful execution
        } catch (Exception e) {
            System.err.println("Error during logoutAllUsers: " + e.getMessage());
            e.printStackTrace();
            return false; // Indicate failure
        }
    }

    // Helper method to update the login status
    private static void updateLoginStatus(String username, boolean isLoggedIn) {
        String updateQuery = "UPDATE User SET isLoggedIn = ? WHERE username = ?";
        executeUpdate(updateQuery, statement -> {
            statement.setBoolean(1, isLoggedIn);
            statement.setString(2, username);
        }, rowsAffected -> {
            if (rowsAffected > 0) {
                System.out.println("Login status updated successfully for user: " + username);
            } else {
                System.out.println("Failed to update login status for user: " + username);
            }
        });
    }

    // Helper method to update login timestamp
    private static void updateLoginTime(String username) {
        String updateQuery = "UPDATE User SET loginTime = NOW() WHERE username = ?";
        executeUpdate(updateQuery, statement -> statement.setString(1, username), rowsAffected -> {
            if (rowsAffected > 0) {
                System.out.println("Login time updated for user: " + username);
            } else {
                System.out.println("Failed to update login time for user: " + username);
            }
        });
    }
    public static void resetPassword(String email, String newPassword) {
        String query = "UPDATE User SET password = ? WHERE email = ?";
        executeUpdate(query, statement -> {
            statement.setString(1, newPassword);
            statement.setString(2, email);
        }, rowsAffected -> {
            if (rowsAffected > 0) {
                System.out.println("Password reset successfully.");
            } else {
                System.out.println("No user found with the provided email.");
            }
        });
    }

    public static void logout(String username) {
        String query = "UPDATE User SET is_logged_in = ?, logoutTime = NOW() WHERE username = ?";
        executeUpdate(query,
                statement -> {
                    statement.setBoolean(1, false); // Set is_logged_in to false
                    statement.setString(2, username);
                },
                rowsAffected -> {
                    if (rowsAffected > 0) {
                        System.out.println("User " + username + " logged out successfully.");
                    } else {
                        System.out.println("Failed to log out user: " + username);
                    }
                }
        );
    }



    public static void viewLoginLogoutHistory(String username) {
        String query = "SELECT loginTime, logoutTime FROM User WHERE username = ?";
        executeRead(query, statement -> statement.setString(1, username), resultSet -> {
            System.out.println("Login/Logout History:");
            if (resultSet.next()) {
                System.out.println("Login Time: " + resultSet.getTimestamp("loginTime"));
                System.out.println("Logout Time: " + resultSet.getTimestamp("logoutTime"));
            } else {
                System.out.println("No history found for this user.");
            }
        });
    }

    public static void viewReadingHistory(String username) {
        String query = "SELECT Title, Category, Rating, Liked, Skipped FROM ReadingHistory WHERE Username = ?";
        executeRead(query, statement -> statement.setString(1, username), resultSet -> {
            System.out.println("Reading History:");
            while (resultSet.next()) {
                System.out.println("Title: " + resultSet.getString("Title"));
                System.out.println("Category: " + resultSet.getString("Category"));
                System.out.println("Rating: " + resultSet.getInt("Rating"));
                System.out.println("Liked: " + (resultSet.getBoolean("Liked") ? "Yes" : "No"));
                System.out.println("Skipped: " + (resultSet.getBoolean("Skipped") ? "Yes" : "No"));
                System.out.println("--------------------------");
            }
        });
    }

    public static void viewFavorites(String username) {
        String query = "SELECT Title, Category, Rating, Liked FROM Favorites WHERE Username = ?";
        executeRead(query, statement -> statement.setString(1, username), resultSet -> {
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
        });
    }

    public static void updateReadingHistory(String username, String title, String category, Integer rating, Boolean liked, Boolean skipped) {
        String updateSql = "UPDATE ReadingHistory SET Rating = ?, Liked = ?, Skipped = ? " +
                "WHERE Username = ? AND Title = ?";
        String insertFavoriteSql = "INSERT INTO Favorites (Username, Title, Category, Rating, Liked) " +
                "VALUES (?, ?, ?, ?, ?)";
        String updateFavoriteSql = "UPDATE Favorites SET Rating = ?, Liked = ? WHERE Username = ? AND Title = ?";

        executeDatabaseTask(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);

                // Update Reading History
                try (PreparedStatement historyStmt = connection.prepareStatement(updateSql)) {
                    historyStmt.setInt(1, rating != null ? rating : 0);
                    historyStmt.setBoolean(2, liked != null && liked);
                    historyStmt.setBoolean(3, skipped != null && skipped);
                    historyStmt.setString(4, username);
                    historyStmt.setString(5, title);
                    historyStmt.executeUpdate();
                }

                // Add or Update Favorites if applicable
                if ((rating != null && rating > 0) || (liked != null && liked)) {
                    try (PreparedStatement checkStmt = connection.prepareStatement(
                            "SELECT * FROM Favorites WHERE Username = ? AND Title = ?")) {
                        checkStmt.setString(1, username);
                        checkStmt.setString(2, title);
                        ResultSet resultSet = checkStmt.executeQuery();

                        if (resultSet.next()) {
                            // Update existing favorite
                            try (PreparedStatement favoriteStmt = connection.prepareStatement(updateFavoriteSql)) {
                                favoriteStmt.setInt(1, rating != null ? rating : 0);
                                favoriteStmt.setBoolean(2, liked != null && liked);
                                favoriteStmt.setString(3, username);
                                favoriteStmt.setString(4, title);
                                favoriteStmt.executeUpdate();
                            }
                        } else {
                            // Insert new favorite
                            try (PreparedStatement favoriteStmt = connection.prepareStatement(insertFavoriteSql)) {
                                favoriteStmt.setString(1, username);
                                favoriteStmt.setString(2, title);
                                favoriteStmt.setString(3, category);
                                favoriteStmt.setInt(4, rating != null ? rating : 0);
                                favoriteStmt.setBoolean(5, liked != null && liked);
                                favoriteStmt.executeUpdate();
                            }
                        }
                    }
                }

                connection.commit();
                System.out.println("Reading history updated and favorite handled for user: " + username);
            } catch (Exception e) {
                System.err.println("Error during reading history update.");
                e.printStackTrace();
            }
        });
    }
    public static void addToReadingHistory(String username, String title, String category, Integer rating, boolean liked, boolean skipped) {
        String sql = "INSERT INTO ReadingHistory (Username, Title, Category, Rating, Liked, Skipped) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, username);
                statement.setString(2, title);
                statement.setString(3, category);
                statement.setInt(4, rating != null ? rating : 0); // Default rating is 0 if null
                statement.setBoolean(5, liked); // Add whether the user liked the article
                statement.setBoolean(6, skipped); // Add whether the user skipped the article

                int rowsInserted = statement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Reading history entry added successfully for user: " + username);
                } else {
                    System.out.println("Failed to add reading history entry for user: " + username);
                }
            } catch (SQLException e) {
                System.err.println("Error while adding to reading history for user: " + username);
                e.printStackTrace();
            }
        });
    }

    // Helper method for executing a query and returning a result
    public static <T> T executeQueryWithResult(String query, PreparedStatementHandler preparedStatementHandler, ResultSetHandlerWithResult<T> resultSetHandler) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            preparedStatementHandler.prepare(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSetHandler.handle(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Execute update method
    public static void executeUpdate(String query, PreparedStatementHandler preparedStatementHandler, UpdateHandler handler) {
        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                preparedStatementHandler.prepare(statement);
                int rowsAffected = statement.executeUpdate();
                handler.handle(rowsAffected);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Execute read method
    public static void executeRead(String query, PreparedStatementHandler preparedStatementHandler, ResultSetHandler handler) {
        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                preparedStatementHandler.prepare(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    handler.handle(resultSet);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Functional interfaces for handlers
    @FunctionalInterface
    public interface PreparedStatementHandler {
        void prepare(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetHandler {
        void handle(ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    public interface UpdateHandler {
        void handle(int rowsAffected) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetHandlerWithResult<T> {
        T handle(ResultSet resultSet) throws SQLException;
    }
}