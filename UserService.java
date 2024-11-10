import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/NewsArticles";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Method to sign up a user
    public void signUp(int userID, String username, String password, String email, String preferences) {
        if (isUserExists(username)) {
            System.out.println("Username already exists.");
            return;
        }
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO User (userID, username, password, email, preferences) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userID);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, email);
            statement.setString(5, preferences);
            statement.executeUpdate();
            System.out.println("User signed up successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to check if a user exists
    private boolean isUserExists(String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT username FROM User WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}