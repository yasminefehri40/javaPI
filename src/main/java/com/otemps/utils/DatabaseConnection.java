package com.otemps.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/otemps";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static Connection instance;

    public static Connection getInstance() {
        if (instance == null) {
            try {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Base de donnees 'otemps' connectee!");
            } catch (SQLException e) {
                System.err.println("Erreur: " + e.getMessage());
            }
        }
        return instance;
    }
}
