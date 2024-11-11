package org.example;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
public class User {
    private int userID;
    private String username;
    private String password;
    private String email;
    private List<String> readingHistory;
    ;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private List<LocalDateTime[]> loginLogoutHistory;

    // Constructor
    public User(int userID, String username, String password, String email) {
        this.userID = userID;
        this.username = username;
        setPassword(password);
        setEmail(email);
        this.loginLogoutHistory = new ArrayList<>();
        this.readingHistory = new ArrayList<>();
    }

    // Getters and Setters
    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    public List<String> getReadingHistory() { return new ArrayList<>(readingHistory); }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) {
        if (password.length() >= 8) {
            this.password = password;
        } else {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
    }
    public void setEmail(String email) {
        if (isValidEmail(email)) {
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email format.");
        }
    }
    public  void addToReadingHistory(String articleTitle){
        this.readingHistory.add(articleTitle);
    }
    // Methods for user actions
    public void login() {
        this.loginTime = LocalDateTime.now();
        System.out.println(username + " logged in at: " + formatDateTime(loginTime));
    }

    public void logout() {
        this.logoutTime = LocalDateTime.now();
        System.out.println(username + " logged out at: " + formatDateTime(logoutTime));
    }
    private void storeLoginLogoutHistory() {
        loginLogoutHistory.add(new LocalDateTime[]{loginTime, logoutTime});
    }
    public void clearLoginLogoutHistory() {
        loginLogoutHistory.clear();
        System.out.println("Login/logout history cleared for " + username);
    }
    public void printLoginLogoutHistory() {
        for (LocalDateTime[] session : loginLogoutHistory) {
            System.out.println("Login: " + formatDateTime(session[0]) + ", Logout: " + formatDateTime(session[1]));
        }
    }
    // Helper methods
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
    public String toString() {
        return "User(" + "UserID=" + userID + ", Username='" + username + "', email='" + email + ")";
    }
}
