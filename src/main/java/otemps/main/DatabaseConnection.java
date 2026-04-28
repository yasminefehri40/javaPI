package otemps.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/otemps";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection cnx;
    private static DatabaseConnection instance;

    private DatabaseConnection() {
        connect();
    }

    private void connect() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Base de donnees 'otemps' connectee pour le module patrimoine.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            cnx = null;
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Erreur verification connexion : " + e.getMessage());
        }
        return cnx;
    }
}
