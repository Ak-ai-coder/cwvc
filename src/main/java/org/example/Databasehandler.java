package org.example;

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

    // Method to demonstrate a database read operation with stacking
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

    // Method to demonstrate a database update operation with stacking
    public static void executeUpdate(String query, UpdateHandler handler) {
        executeDatabaseTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                int rowsAffected = statement.executeUpdate();
                handler.handle(rowsAffected);
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

    // Functional interface for handling the result of an update
    @FunctionalInterface
    public interface UpdateHandler {
        void handle(int rowsAffected) throws SQLException;
    }
}