package org.example;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseHandlerTest {

    @Test
    void testDatabaseConnection() {
        try (Connection connection = Databasehandler.getConnection()) {
            assertNotNull(connection, "Connection should not be null.");
            System.out.println("Database connection successful.");
        } catch (SQLException e) {
            fail("Connection failed: " + e.getMessage());
        }
    }


    @Test
    void testGetTablesFromDatabase() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/NewsArticles", "root", "")) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables("NewsArticles", null, "%", new String[]{"TABLE"});

            boolean hasTables = false;
            System.out.println("Tables in NewsArticles database:");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("- " + tableName);
                hasTables = true;
            }

            if (!hasTables) {
                System.out.println("No tables found in the NewsArticles database.");
            }

            assertTrue(hasTables, "The NewsArticles database should contain tables.");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Connection failed: " + e.getMessage());
        }
    }
}