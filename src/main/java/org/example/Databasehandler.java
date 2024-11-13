package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Databasehandler {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Single-threaded executor for stacking database requests
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public static void executeDatabaseTask(Runnable task) {
        dbExecutor.submit(task);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void shutdown() {
        dbExecutor.shutdown();
    }

    // Example method to demonstrate a database read operation
    public static void executeRead(String query, ResultSetHandler handler) {
        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                handler.handle(resultSet);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Example method to demonstrate a database update operation
    public static void executeUpdate(String query) {
        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Functional interface for handling ResultSet
    @FunctionalInterface
    public interface ResultSetHandler {
        void handle(ResultSet resultSet) throws SQLException;
    }
}
