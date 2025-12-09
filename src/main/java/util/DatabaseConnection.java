package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Connexion partagée (singleton)
    private static Connection connection;

    // ⚠️ ADAPTE si besoin (nom BDD, user, mot de passe)
    private static final String URL      = "jdbc:mysql://localhost:3306/inventoryfx?serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = ""; // ou "root" si c'est ton cas

    // Premier chargement : on essaie d'ouvrir une connexion
    static {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✔ Connected to MySQL");
        } catch (SQLException e) {
            System.err.println("❌ Erreur connexion MySQL (static init) : " + e.getMessage());
        }
    }

    /**
     * Retourne une connexion OU en recrée une si elle a été fermée.
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // 👉 Si quelqu'un l'a fermée, on la recrée ici
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✔ (re)Connected to MySQL");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getConnection() : " + e.getMessage());
        }
        return connection;
    }

    // (Optionnel : une méthode pour fermer à la fin de l’appli si tu veux)
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✖ MySQL connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}
