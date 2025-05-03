package com.rhms.ui;

import com.rhms.userManagement.*;
import com.rhms.Database.UserDatabaseHandler;
import java.util.Scanner;

public class LoginHandler {
    private final UserManager userManager;
    private final UserDatabaseHandler dbHandler;

    public LoginHandler(UserManager userManager) {
        this.userManager = userManager;
        this.dbHandler = new UserDatabaseHandler();
    }

    public User login(Scanner scanner) {
        while (true) {
            System.out.println("\n===== Login =====");
            System.out.print("Enter username (or '0' to go back): ");
            String username = scanner.nextLine().trim();

            if (username.equals("0")) {
                return null;
            }

            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            User user = dbHandler.getUserByUsername(username);
            if (user != null && user.getPassword().equals(password)) {
                String userType = getUserType(user);
                System.out.println("Login successful. Welcome, " + userType + " " + user.getName() + "!");
                return user;
            } else {
                System.out.println("Login failed. Invalid username or password.");
                System.out.println("Would you like to try again? (y/n): ");
                String retry = scanner.nextLine().trim().toLowerCase();
                if (!retry.equals("y")) {
                    return null;
                }
            }
        }
    }

    private String getUserType(User user) {
        if (user instanceof Administrator) {
            return "Administrator";
        } else if (user instanceof Doctor) {
            return "Dr.";
        } else if (user instanceof Patient) {
            return "Patient";
        } else {
            return "";
        }
    }
}
