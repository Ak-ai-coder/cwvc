package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Cli {
    public static void main(String[] args) {
        UserService userService = new UserService();
        Scanner scanner = new Scanner(System.in);
        String loggedInUsername = null;

        // Initialize articles (simplified for this version)
        List<JSONObject> articles = new ArrayList<>();

        // Display menu
        System.out.println("1. Sign Up");
        System.out.println("2. Login");
        System.out.println("3. View Reading History");
        System.out.println("4. Generate Recommendations");
        System.out.println("5. Exit");
        // Implement the logic for each case
    }
}